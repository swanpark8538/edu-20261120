#!/usr/bin/env node
// 게시판 과제 채점기. TASK.md 의 인수 조건(HTTP 계약)을 블랙박스로 검사하고,
// 품질 항목은 소스 정적 분석과 HTTP 검사를 섞어 판정한다.
//
// 사용법: node scripts/verify.mjs [projectDir=board-app] [--label 이름] [--log F.jsonl] [--json out.json] [--keep] [--static]
//   npm run verify            ← 수강생은 이것만 실행하면 된다.
//   npm run verify -- --static   빌드·기동 없이 정적 항목(Q1·Q4·Q5·Q6 일부·S1)만 빠르게 확인 (기록 없음)
//
// 대상 프로젝트를 임시 디렉터리로 복사해서 빌드·기동하므로, IDE 에서 띄워 둔 앱이나 target/ 과
// 충돌하지 않는다. .participant.json 이 있으면 결과 한 줄을 results/human/<ID>/progress.jsonl 에
// 이어 붙인다 (시간에 따른 마일스톤 도달 곡선용).
//
// 마일스톤 (총 16)
//   기본      B1 빌드 → B2 기동
//   기능      F1 등록(C) → F2 조회(R) → F3 수정(U) → F4 삭제(D) → F5 업로드 → F6 다운로드 → F7 업로드 보안
//   품질      Q1 Config 클래스 → Q2 페이징 → Q3 검색 → Q4 트랜잭션 → Q5 ID 생성 → Q6 예외/검증/i18n
//   표준 준수 S1 표준프레임워크 적용률 (egovframe-mcp-check 의 egov_check_compliance 를 직접 실행)
import { spawn } from "node:child_process";
import { createServer } from "node:net";
import { randomBytes } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { KIT_DIR, PROJECT_DIR, RESULTS_DIR, MVN, checkEntry, readParticipant } from "../config/kit.config.mjs";
import { MILESTONES } from "../config/milestones.mjs";

// ─── 인자 ──────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);
const opt = (name) => {
  const i = argv.indexOf(name);
  return i >= 0 ? argv[i + 1] : undefined;
};
const projectDir = argv[0] && !argv[0].startsWith("--") ? path.resolve(argv[0]) : PROJECT_DIR;
if (!fs.existsSync(path.join(projectDir, "pom.xml"))) {
  console.error(`pom.xml 이 없습니다: ${projectDir}`);
  console.error("usage: verify.mjs [projectDir] [--label L] [--log F] [--json F] [--keep]");
  process.exit(1);
}
const participant = readParticipant();
const label = opt("--label") ?? participant?.id ?? path.basename(projectDir);
const logFile =
  opt("--log") ??
  (participant && !argv.includes("--static") && path.resolve(projectDir) === PROJECT_DIR
    ? path.join(RESULTS_DIR, "human", participant.id, "progress.jsonl")
    : undefined);
const jsonFile = opt("--json");
const keep = argv.includes("--keep");
const staticOnly = argv.includes("--static"); // 빌드·기동 없이 정적 항목만 (기록 없음)
const CHECK_MCP = checkEntry();

const MB = 1024 * 1024;
const BUILD_TIMEOUT_MS = 8 * 60_000;
const BOOT_TIMEOUT_MS = 150_000;
const PAGE_UNIT = 10; // EgovConfigProperties 의 pageUnit

// ─── 마일스톤 정의 (config/milestones.mjs) ───────────────────────────
const startedAt = new Date();
const results = {};
let currentGroup = "";
const record = (id, pass, detail = "") => {
  const [, group, name] = MILESTONES.find(([m]) => m === id);
  if (group !== currentGroup) {
    currentGroup = group;
    console.log(`\n[${group}]`);
  }
  results[id] = { pass, detail, at: new Date().toISOString() };
  console.log(`${pass ? "✅" : "❌"} ${id} ${name}${detail ? ` — ${detail}` : ""}`);
};
const skip = (id, why) => record(id, false, why);

// ─── 프로세스·파일 유틸 ────────────────────────────────────────────────
function run(cmd, args, { cwd, timeoutMs, logPath }) {
  return new Promise((resolve) => {
    const out = fs.openSync(logPath, "w");
    const p = spawn(cmd, args, { cwd, stdio: ["ignore", out, out], shell: false });
    const t = setTimeout(() => p.kill("SIGKILL"), timeoutMs);
    p.on("close", (code) => {
      clearTimeout(t);
      fs.closeSync(out);
      resolve(code);
    });
    p.on("error", (e) => {
      fs.writeSync(out, `실행 실패: ${e.message}\n`);
      resolve(-1);
    });
  });
}
const tail = (file, n = 15) => {
  try {
    return fs.readFileSync(file, "utf8").trimEnd().split("\n").slice(-n).join("\n");
  } catch {
    return "";
  }
};
const freePort = () =>
  new Promise((resolve, reject) => {
    const s = createServer();
    s.listen(0, "127.0.0.1", () => {
      const { port } = s.address();
      s.close(() => resolve(port));
    });
    s.on("error", reject);
  });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function walk(dir, pred, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, pred, acc);
    else if (pred(p)) acc.push(p);
  }
  return acc;
}
const read = (f) => fs.readFileSync(f, "utf8");
// 주석 줄을 제외한 소스 (정적 검사가 주석에 속지 않도록)
const stripComments = (src) => src.replace(/\/\*[\s\S]*?\*\//g, "").replace(/^\s*\/\/.*$/gm, "");

// ─── HTTP 세션 (쿠키 + CSRF) ───────────────────────────────────────────
class Session {
  constructor(base) {
    this.base = base;
    this.cookies = new Map();
  }
  cookieHeader() {
    return [...this.cookies].map(([k, v]) => `${k}=${v}`).join("; ");
  }
  absorb(res) {
    for (const c of res.headers.getSetCookie?.() ?? []) {
      const [kv] = c.split(";");
      const i = kv.indexOf("=");
      this.cookies.set(kv.slice(0, i).trim(), kv.slice(i + 1).trim());
    }
  }
  async req(method, url, { body, headers = {} } = {}) {
    const full = url.startsWith("http") ? url : this.base + url;
    const res = await fetch(full, {
      method,
      body,
      redirect: "manual",
      headers: { Cookie: this.cookieHeader(), "Accept-Language": "ko-KR", ...headers },
      signal: AbortSignal.timeout(60_000),
    });
    this.absorb(res);
    const buf = Buffer.from(await res.arrayBuffer());
    return { status: res.status, headers: res.headers, buf, text: buf.toString("utf8") };
  }
  // 3xx 면 한 번 따라가 최종 응답을 돌려준다.
  async follow(r) {
    const loc = r.headers.get("location");
    if (r.status >= 300 && r.status < 400 && loc) return this.req("GET", loc);
    return r;
  }
  get(url) {
    return this.req("GET", url);
  }
  async postMultipart(url, fields, files = []) {
    const { body, contentType } = buildMultipart({ ...fields, _csrf: this.csrf }, files);
    return this.req("POST", url, { body, headers: { "Content-Type": contentType } });
  }
  takeCsrf(html) {
    const m =
      html.match(/<meta[^>]*name="_csrf"[^>]*content="([^"]+)"/) ??
      html.match(/<input[^>]*name="_csrf"[^>]*value="([^"]+)"/) ??
      html.match(/<input[^>]*value="([^"]+)"[^>]*name="_csrf"/);
    if (m) this.csrf = m[1];
    return Boolean(m);
  }
}

// 브라우저와 같은 형태의 multipart 본문. 파일을 고르지 않은 file 필드는 filename="" 빈 파트로 보낸다.
function buildMultipart(fields, files) {
  const boundary = "----verify" + randomBytes(8).toString("hex");
  const chunks = [];
  for (const [k, v] of Object.entries(fields)) {
    if (v === undefined) continue;
    chunks.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${v}\r\n`));
  }
  for (const f of files) {
    chunks.push(
      Buffer.from(
        `--${boundary}\r\nContent-Disposition: form-data; name="${f.field}"; filename="${f.filename}"\r\n` +
          `Content-Type: ${f.contentType ?? "application/octet-stream"}\r\n\r\n`,
      ),
      f.data,
      Buffer.from("\r\n"),
    );
  }
  chunks.push(Buffer.from(`--${boundary}--\r\n`));
  return { body: Buffer.concat(chunks), contentType: `multipart/form-data; boundary=${boundary}` };
}

const decodeHtml = (s) =>
  s.replace(/&amp;/g, "&").replace(/&#39;/g, "'").replace(/&quot;/g, '"').replace(/&lt;/g, "<").replace(/&gt;/g, ">");

// 목록 HTML 의 한 행(<tr>)에서 게시글 ID 를 뽑는다. onclick 인자 → boardId 쿼리 → 첫 셀 순.
function rowId(row) {
  const r = decodeHtml(row);
  const m =
    r.match(/on\w+\s*=\s*"[^"]*?\w+\(\s*['"]?([A-Za-z0-9_\-]+)['"]?\s*[,)]/) ??
    r.match(/boardId=([A-Za-z0-9_\-]+)/) ??
    r.match(/<td[^>]*>(?:\s*<[^>]+>)*\s*([A-Za-z0-9_\-]+)\s*</);
  return m?.[1];
}
const rows = (html) => html.split(/<tr[\s>]/i).slice(1);
function findRowId(html, title) {
  for (const row of rows(html)) if (row.includes(title)) return rowId(row);
  return undefined;
}
const allRowIds = (html) => new Set(rows(html).map(rowId).filter(Boolean));

function findDownloadLinks(html) {
  const links = new Set();
  for (const m of html.matchAll(/href="([^"]+)"/g)) {
    const u = decodeHtml(m[1]);
    if (/FileDown|atchFileId=|download/i.test(u)) links.add(u);
  }
  return [...links];
}
const pick = (html, name) => {
  const m =
    html.match(new RegExp(`<input[^>]*name="${name}"[^>]*value="([^"]*)"`)) ??
    html.match(new RegExp(`<input[^>]*value="([^"]*)"[^>]*name="${name}"`));
  return m ? decodeHtml(m[1]) : undefined;
};
const isServerError = (r) => r.status >= 500;
const looksLikeCrash = (html) => /Whitelabel Error Page/i.test(html) || /java\.lang\.\w+Exception|\bat [a-z]+(\.[\w$]+)+\(/.test(html);

// ─── 정적 검사 (소스 기반, 빌드와 무관) ─────────────────────────────────
function staticChecks(proj) {
  const javaRoot = path.join(proj, "src", "main", "java");
  const resRoot = path.join(proj, "src", "main", "resources");
  const java = walk(javaRoot, (f) => f.endsWith(".java"));
  const boardJava = java.filter((f) => /board/i.test(path.relative(javaRoot, f)));
  const rel = (f) => path.relative(proj, f).split(path.sep).join("/");

  // Q1 @Configuration 클래스에 Board 설정
  const configs = java.filter((f) => {
    const s = stripComments(read(f));
    return /@Configuration\b/.test(s) && /\bBoard\b|Board[A-Z]/.test(s);
  });
  const q1 = { pass: configs.length > 0, detail: configs.length ? configs.map((f) => path.basename(f)).join(", ") : "Board 관련 @Configuration 클래스가 없음 (예: EgovConfigBoardIdGeneration)" };

  // Q4 트랜잭션: Board ServiceImpl 이 @Transactional 이거나 TransactionInterceptor 포인트컷에 포함
  const impl = boardJava.find((f) => /Board\w*ServiceImpl\.java$/.test(f)) ?? java.find((f) => /Board\w*ServiceImpl\.java$/.test(f));
  let q4;
  if (!impl) q4 = { pass: false, detail: "Board ServiceImpl 클래스를 찾지 못함" };
  else {
    const src = stripComments(read(impl));
    const pkg = src.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1] ?? "";
    const fqn = `${pkg}.${path.basename(impl, ".java")}`;
    if (/@Transactional\b/.test(src)) q4 = { pass: true, detail: `${path.basename(impl)} 에 @Transactional` };
    else {
      const txFiles = java.filter((f) => /TransactionInterceptor|txAdvice|EnableTransactionManagement/.test(stripComments(read(f))));
      const patterns = [];
      for (const f of txFiles)
        for (const m of stripComments(read(f)).matchAll(/execution\s*\(\s*\*?\s*([\w.*]+)\s*\(/g)) patterns.push({ file: path.basename(f), expr: m[1] });
      const hit = patterns.find((p) => aspectjMatches(p.expr, `${fqn}.insertBoard`));
      q4 = hit
        ? { pass: true, detail: `${hit.file} 의 포인트컷 ${hit.expr} 이 ${path.basename(impl)} 을 포함` }
        : {
            pass: false,
            detail: patterns.length
              ? `포인트컷 ${patterns.map((p) => p.expr).join(" / ")} 이 ${fqn} 을 포함하지 않음 (@Transactional 도 없음)`
              : "트랜잭션 포인트컷을 찾지 못함",
          };
    }
  }

  // Q5(정적) Board 서비스가 표준 ID Generation 서비스 사용
  const idgnr = boardJava.filter((f) => /IdGnrService|EgovTableIdGnrServiceImpl|getNextStringId/.test(stripComments(read(f))));
  const q5s = { pass: idgnr.length > 0, detail: idgnr.length ? `${idgnr.map((f) => path.basename(f)).join(", ")} 에서 EgovIdGnrService 사용` : "Board 패키지에서 EgovIdGnrService(getNextStringId) 사용 흔적 없음" };

  // Q6(정적) i18n: 메시지 번들 3종에 board 키 + 화면이 #{...} 메시지 표현식 사용
  const bundles = walk(resRoot, (f) => /message[^/\\]*\.properties$/.test(f));
  const bundleHasBoard = (f) => /^[^#\n]*board[^=\n]*=/im.test(read(f));
  const withBoard = bundles.filter(bundleHasBoard);
  const locales = new Set(withBoard.map((f) => path.basename(f).match(/_([a-z]{2})\.properties$/)?.[1] ?? "base"));
  const boardViews = walk(resRoot, (f) => /templates[\\/].*board.*\.html$/i.test(f) || /templates[\\/].*[\\/]board[\\/].*\.html$/i.test(f));
  const viewMsgExprs = boardViews.reduce((n, f) => n + (read(f).match(/#\{[\w.]+\}/g)?.length ?? 0), 0);
  const i18nProblems = [];
  if (!(locales.has("base") && locales.has("ko") && locales.has("en"))) i18nProblems.push(`board 메시지 키가 있는 번들: ${[...locales].join(",") || "없음"} (base/ko/en 모두 필요)`);
  if (!boardViews.length) i18nProblems.push("board 화면 템플릿을 찾지 못함");
  else if (viewMsgExprs < 3) i18nProblems.push(`board 화면의 메시지 표현식 #{...} ${viewMsgExprs}개 (3개 이상 필요)`);
  const i18n = { pass: i18nProblems.length === 0, detail: i18nProblems.join("; ") };

  // S1 보조: Board 계층 파일 수 (check 결과에 Board 가 포함되었는지 확인용)
  const boardLayerFiles = java.filter((f) => /Board\w*(Controller|ServiceImpl|DAO|Mapper)\.java$/.test(f)).map(rel);

  return { q1, q4, q5s, i18n, boardLayerFiles };
}

// AspectJ execution 패턴의 타입+메서드 부분을 정규식으로 바꾸어 "FQN.method" 와 대조한다.
//   ".." → 0개 이상의 패키지,  "*" → 식별자 문자 0개 이상
function aspectjMatches(expr, target) {
  let re = "";
  for (let i = 0; i < expr.length; i++) {
    const c = expr[i];
    if (c === "." && expr[i + 1] === ".") {
      re += "\\.(?:[\\w$]+\\.)*";
      i++;
    } else if (c === ".") re += "\\.";
    else if (c === "*") re += "[\\w$]*";
    else re += c.replace(/[$]/g, "\\$");
  }
  return new RegExp(`^${re}$`).test(target);
}

async function complianceCheck(proj, boardLayerFiles) {
  if (!fs.existsSync(CHECK_MCP)) throw new Error(`egovframe-mcp-check 를 찾지 못함 (${CHECK_MCP}). npm run setup 을 먼저 실행하세요.`);
  const client = new Client({ name: "verify", version: "1.0.0" });
  await client.connect(new StdioClientTransport({ command: process.execPath, args: [CHECK_MCP], stderr: "ignore" }));
  const res = await client.callTool({ name: "egov_check_compliance", arguments: { projectPath: proj } });
  await client.close();
  const text = (res.content ?? []).map((c) => c.text ?? "").join("\n");
  const total = Number(text.match(/Total Files:\s*(\d+)/)?.[1] ?? 0);
  const failed = Number(text.match(/Failed:\s*(\d+)/)?.[1] ?? NaN);
  const rate = Number(text.match(/Compliance Rate:\s*(\d+)%/)?.[1] ?? NaN);
  const failedFiles = [...text.matchAll(/✗\s+(\S+\.java):\s*([^\n]+)/g)].map((m) => `${m[1]}(${m[2].trim()})`);
  const problems = [];
  if (res.isError) problems.push("도구가 오류를 반환");
  if (!(failed === 0)) problems.push(`위반 ${Number.isNaN(failed) ? "?" : failed}건${failedFiles.length ? ": " + failedFiles.slice(0, 5).join(", ") : ""}`);
  if (boardLayerFiles.length < 3) problems.push(`Board 계층 파일 ${boardLayerFiles.length}개 (Controller/ServiceImpl/Mapper 3개 필요)`);
  return {
    pass: problems.length === 0,
    detail: `적용률 ${Number.isNaN(rate) ? "?" : rate + "%"} (검사 ${total}개, Board 계층 ${boardLayerFiles.length}개)${problems.length ? " — " + problems.join("; ") : ""}`,
    rate,
    total,
    failed,
  };
}

// ─── 본 검사 ───────────────────────────────────────────────────────────
const work = fs.mkdtempSync(path.join(os.tmpdir(), "egov-verify-"));
let app;
let compliance;

async function main() {
  console.log(`▶ 채점 대상: ${projectDir}`);
  const proj = path.join(work, "project");
  fs.cpSync(projectDir, proj, {
    recursive: true,
    filter: (src) => !/[\\/](target|\.git|node_modules|\.mcp-servers)$/.test(src),
  });
  const st = staticChecks(proj);

  // B1 빌드
  const buildLog = path.join(work, "build.log");
  const code = staticOnly ? -2 : await run(MVN, ["-q", "-B", "-DskipTests", "package"], { cwd: proj, timeoutMs: BUILD_TIMEOUT_MS, logPath: buildLog });
  let jar;
  if (staticOnly) record("B1", false, "--static: 빌드 생략");
  else if (code === 0) {
    const t = path.join(proj, "target");
    jar = fs.existsSync(t) ? fs.readdirSync(t).find((f) => f.endsWith(".jar") && !f.endsWith(".original")) : undefined;
  }
  if (!staticOnly) record("B1", Boolean(jar), jar ? "" : `mvn 종료 코드 ${code}\n${tail(buildLog)}`);

  // B2 기동
  let s;
  if (jar) {
    const port = await freePort();
    const appLog = path.join(work, "app.log");
    const store = path.join(work, "filestore");
    fs.mkdirSync(store);
    const out = fs.openSync(appLog, "w");
    app = spawn("java", ["-jar", path.join(proj, "target", jar), `--server.port=${port}`, `--Globals.fileStorePath=${store}${path.sep}`], {
      cwd: proj,
      stdio: ["ignore", out, out],
      shell: false,
    });
    let exited = false;
    app.on("exit", () => (exited = true));
    s = new Session(`http://127.0.0.1:${port}`);
    const deadline = Date.now() + BOOT_TIMEOUT_MS;
    let last;
    while (Date.now() < deadline && !exited) {
      try {
        last = await s.get("/egovBoardList.do"); // 내장 Tomcat 은 컨텍스트 준비 후 포트를 열므로 응답이 오면 기동 완료
        break;
      } catch {}
      await sleep(1500);
    }
    const ok = last?.status === 200;
    record("B2", ok, ok ? "" : exited ? `앱이 종료됨\n${tail(appLog)}` : `응답 상태 ${last?.status ?? "없음"}`);
    if (ok && !s.takeCsrf(last.text)) {
      const d = await s.get("/egovDeptList.do");
      s.takeCsrf(d.text);
      console.log("   ⚠️  게시판 목록 화면에서 CSRF 토큰을 찾지 못해 Dept 화면 토큰을 사용했습니다.");
    }
    if (!ok) s = undefined;
  } else skip("B2", staticOnly ? "--static: 기동 생략" : "B1 실패로 건너뜀");

  const stamp = Date.now().toString(36);
  const list = async (qs = "") => (await s.get(`/egovBoardList.do${qs}`)).text;
  const emptyFile = { field: "file_1", filename: "", data: Buffer.alloc(0) };
  const add = (title, files = [emptyFile], extra = {}) =>
    s.postMultipart("/addBoard.do", { title, content: `본문 ${title}`, writer: "verify", ...extra }, files);
  const updateView = (id) => s.postMultipart("/updateBoardView.do", { boardId: id });
  const del = (id) => s.postMultipart("/deleteBoard.do", { boardId: id });

  // ── 기능: CRUD ─────────────────────────────────────────────────────
  if (s) {
    const t1 = `verify-crud-${stamp}`;
    let id;
    try {
      const r = await add(t1);
      id = findRowId(await list(), t1);
      record("F1", r.status < 400 && Boolean(id), r.status >= 400 ? `등록 응답 ${r.status}` : id ? "" : "등록 후 목록에서 게시글(ID)을 찾지 못함");
    } catch (e) {
      record("F1", false, `요청 오류: ${e.message}`);
    }
    let v;
    if (id) {
      try {
        v = await updateView(id);
        record("F2", v.status === 200 && v.text.includes(t1), v.status !== 200 ? `수정 화면 응답 ${v.status}` : v.text.includes(t1) ? "" : "수정 화면에 등록한 제목이 표시되지 않음");
      } catch (e) {
        record("F2", false, `요청 오류: ${e.message}`);
      }
    } else skip("F2", "F1 실패로 건너뜀");
    const t2 = `${t1}-edited`;
    if (id && results.F2.pass) {
      try {
        const u = await s.postMultipart(
          "/updateBoard.do",
          { boardId: id, title: t2, content: "수정됨", writer: "verify", atchFileId: pick(v.text, "atchFileId") ?? "" },
          [emptyFile],
        );
        const html = await list();
        record("F3", u.status < 400 && html.includes(t2), u.status >= 400 ? `수정 응답 ${u.status}` : html.includes(t2) ? "" : "수정한 제목이 목록에 없음");
      } catch (e) {
        record("F3", false, `요청 오류: ${e.message}`);
      }
    } else skip("F3", "F2 실패로 건너뜀");
    if (id && results.F3.pass) {
      try {
        const d = await del(id);
        const html = await list();
        record("F4", d.status < 400 && !html.includes(t2), d.status >= 400 ? `삭제 응답 ${d.status}` : html.includes(t2) ? "삭제 후에도 목록에 남아 있음" : "");
      } catch (e) {
        record("F4", false, `요청 오류: ${e.message}`);
      }
    } else skip("F4", "F3 실패로 건너뜀");
  } else for (const m of ["F1", "F2", "F3", "F4"]) skip(m, staticOnly ? "--static: 생략" : "B2 실패로 건너뜀");

  // ── 기능: 첨부 (2MB 정상 파일) ─────────────────────────────────────
  if (s) {
    const t = `verify-attach-${stamp}`;
    const data = randomBytes(2 * MB);
    let links = [];
    try {
      const r = await add(t, [{ field: "file_1", filename: "verify-2mb.pdf", contentType: "application/pdf", data }]);
      const id = findRowId(await list(), t);
      if (r.status >= 400 || !id) record("F5", false, `첨부 등록 실패 (응답 ${r.status}${id ? "" : ", 목록에 없음"})`);
      else {
        links = findDownloadLinks((await updateView(id)).text);
        record("F5", links.length > 0, links.length ? "" : "수정 화면에 첨부 다운로드 링크(<a href>)가 없음");
      }
    } catch (e) {
      record("F5", false, `요청 오류: ${e.message}`);
    }
    if (results.F5.pass) {
      try {
        let matched = false;
        let disp = "";
        for (const l of links) {
          const dl = await s.get(l);
          if (dl.status === 200 && dl.buf.equals(data)) {
            matched = true;
            disp = dl.headers.get("content-disposition") ?? "";
            break;
          }
        }
        record("F6", matched && /attachment/i.test(disp), !matched ? "다운로드 내용이 업로드한 파일과 다름" : /attachment/i.test(disp) ? "" : `Content-Disposition 이 attachment 가 아님 (${disp || "없음"})`);
      } catch (e) {
        record("F6", false, `요청 오류: ${e.message}`);
      }
    } else skip("F6", "F5 실패로 건너뜀");

    // F7 업로드 보안 — 업로드가 동작할 때만 의미가 있다.
    if (results.F5.pass) {
      const problems = [];
      try {
        const te = `verify-exe-${stamp}`;
        const exe = Buffer.concat([Buffer.from("MZ"), randomBytes(1024)]);
        const re = await add(te, [{ field: "file_1", filename: "evil.exe", data: exe }]);
        if (isServerError(re)) problems.push(`exe 업로드 시 ${re.status} 오류 화면`);
        const ide = findRowId(await list(), te);
        if (ide) {
          for (const l of findDownloadLinks((await updateView(ide)).text)) {
            const dl = await s.get(l);
            if (dl.buf.equals(exe)) problems.push("exe 파일이 저장되어 다운로드됨");
          }
        }
        const tb = `verify-big-${stamp}`;
        const rb = await add(tb, [{ field: "file_1", filename: "big.pdf", contentType: "application/pdf", data: randomBytes(11 * MB) }]);
        if (isServerError(rb)) problems.push(`10MB 초과 업로드 시 ${rb.status} 오류 화면`);
        if (findRowId(await list(), tb)) problems.push("10MB 초과 파일의 게시글이 등록됨");
      } catch (e) {
        problems.push(`요청 오류: ${e.message}`);
      }
      record("F7", problems.length === 0, problems.join(", "));
    } else skip("F7", "F5 실패로 건너뜀");
  } else for (const m of ["F5", "F6", "F7"]) skip(m, staticOnly ? "--static: 생략" : "B2 실패로 건너뜀");

  // ── 품질 ──────────────────────────────────────────────────────────
  record("Q1", st.q1.pass, st.q1.detail);

  if (s && results.F1.pass) {
    // Q2 페이징: 12건 등록 → 1페이지에 전부 보이지 않고, 2페이지와 합치면 전부 보인다.
    const needle = `needle${randomBytes(3).toString("hex")}`;
    const titles = Array.from({ length: PAGE_UNIT + 2 }, (_, i) => `pg-${stamp}-${String(i + 1).padStart(2, "0")}${i === 6 ? "-" + needle : ""}`);
    try {
      for (const t of titles) await add(t);
      const p1 = await list();
      const p2 = await list("?pageIndex=2");
      const in1 = titles.filter((t) => p1.includes(t));
      const in2 = titles.filter((t) => p2.includes(t));
      const union = new Set([...in1, ...in2]);
      const hasNav = /pageIndex|pagination|paging/i.test(p1);
      const problems = [];
      if (in1.length >= titles.length) problems.push(`1페이지에 ${titles.length}건이 모두 표시됨 (분할 안 됨)`);
      if (union.size < titles.length) problems.push(`1·2페이지를 합쳐도 ${union.size}/${titles.length}건만 표시`);
      if (!hasNav) problems.push("페이지 이동 요소(pageIndex)가 없음");
      record("Q2", problems.length === 0, problems.length ? problems.join("; ") : `1페이지 ${in1.length}건 + 2페이지 ${in2.length}건`);
    } catch (e) {
      record("Q2", false, `요청 오류: ${e.message}`);
    }
    // Q3 검색: needle 로 검색하면 그 글만 나온다.
    try {
      const target = titles[6];
      const other = titles[0];
      let hit;
      for (const cond of ["0", "1", "2", ""]) {
        const qs = `?searchKeyword=${needle}${cond === "" ? "" : `&searchCondition=${cond}`}`;
        const html = await list(qs);
        if (html.includes(target) && !html.includes(other)) {
          hit = qs;
          break;
        }
      }
      const none = await list(`?searchCondition=0&searchKeyword=zz${needle}zz`);
      record("Q3", Boolean(hit) && !isServerErrorHtml(none), hit ? `${hit}` : "searchKeyword 로 요청해도 목록이 필터링되지 않음 (searchCondition 0/1/2 모두 시도)");
    } catch (e) {
      record("Q3", false, `요청 오류: ${e.message}`);
    }
  } else {
    skip("Q2", staticOnly ? "--static: 생략" : s ? "F1 실패로 건너뜀" : "B2 실패로 건너뜀");
    skip("Q3", staticOnly ? "--static: 생략" : s ? "F1 실패로 건너뜀" : "B2 실패로 건너뜀");
  }

  record("Q4", st.q4.pass, st.q4.detail);

  // Q5 ID 생성: 등록 화면에 사용자 입력 ID 란이 없고(동적) + 서버가 채번했고(F1) + EgovIdGnrService 사용(정적)
  {
    const problems = [];
    if (!st.q5s.pass) problems.push(st.q5s.detail);
    if (s) {
      try {
        const form = (await s.postMultipart("/addBoardView.do", {})).text;
        const inputs = [...form.matchAll(/<input[^>]*name="boardId"[^>]*>/g)].map((m) => m[0]);
        const editable = inputs.filter((i) => !/type="hidden"|readonly|disabled/.test(i));
        if (editable.length) problems.push("등록 화면에 사용자가 입력하는 boardId 입력란이 있음");
      } catch (e) {
        problems.push(`등록 화면 요청 오류: ${e.message}`);
      }
      if (!results.F1.pass) problems.push("등록(F1)이 실패해 채번 결과를 확인할 수 없음");
    } else problems.push(staticOnly ? "--static: 동적 확인 생략" : "B2 실패로 동적 확인 생략");
    record("Q5", problems.length === 0, problems.length ? problems.join("; ") : st.q5s.detail);
  }

  // Q6 예외 처리 + 입력 검증 + i18n
  {
    const problems = [];
    if (!st.i18n.pass) problems.push(`[i18n] ${st.i18n.detail}`);
    if (s) {
      try {
        // 검증: 빈 제목으로 등록 → 5xx 가 아니고, 목록에 새 행이 생기지 않아야 한다.
        const before = allRowIds(await list());
        const r = await s.follow(await add("", [emptyFile], { content: `invalid-${stamp}`, writer: "verify" }));
        const after = allRowIds(await list());
        const created = [...after].some((x) => !before.has(x));
        if (isServerError(r) || looksLikeCrash(r.text)) problems.push(`[검증] 빈 제목 등록 시 ${r.status} 오류 화면`);
        else if (created) problems.push("[검증] 빈 제목인데 게시글이 등록됨 (@Valid 검증 없음)");
        // 예외: 없는 ID 조회 → 스택트레이스/Whitelabel 이 아닌 안내 화면
        const bad = await s.follow(await updateView(`NOPE-${stamp}`));
        if (looksLikeCrash(bad.text)) problems.push(`[예외] 없는 ID 조회 시 처리되지 않은 오류 화면 (${bad.status})`);
      } catch (e) {
        problems.push(`요청 오류: ${e.message}`);
      }
    } else problems.push(staticOnly ? "--static: 동적 확인 생략" : "B2 실패로 동적 확인 생략");
    record("Q6", problems.length === 0, problems.join("; "));
  }

  // ── 표준 준수 ─────────────────────────────────────────────────────
  try {
    compliance = await complianceCheck(proj, st.boardLayerFiles);
    record("S1", compliance.pass, compliance.detail);
  } catch (e) {
    record("S1", false, `검사 실행 실패: ${e.message}`);
  }
}
const isServerErrorHtml = (html) => looksLikeCrash(html);

try {
  await main();
} finally {
  if (app && app.exitCode === null) app.kill("SIGTERM");
  const groups = {};
  for (const [id, g] of MILESTONES) {
    groups[g] ??= { pass: 0, total: 0 };
    groups[g].total++;
    if (results[id]?.pass) groups[g].pass++;
  }
  const passed = MILESTONES.filter(([m]) => results[m]?.pass).length;
  const summary = {
    label,
    project: projectDir,
    startedAt: startedAt.toISOString(),
    finishedAt: new Date().toISOString(),
    score: passed,
    maxScore: MILESTONES.length,
    groups,
    complianceRate: compliance?.rate ?? null,
    milestones: results,
  };
  console.log(`\n점수: ${passed}/${MILESTONES.length}  (${Object.entries(groups).map(([g, v]) => `${g} ${v.pass}/${v.total}`).join(" · ")})`);
  if (compliance && !Number.isNaN(compliance.rate)) console.log(`표준프레임워크 적용률: ${compliance.rate}%`);
  if (logFile) {
    fs.mkdirSync(path.dirname(logFile), { recursive: true });
    fs.appendFileSync(logFile, JSON.stringify(summary) + "\n");
    const relLog = path.relative(KIT_DIR, logFile);
    console.log(`기록: ${relLog.startsWith("..") ? logFile : relLog}`);
  }
  if (jsonFile) {
    fs.mkdirSync(path.dirname(jsonFile), { recursive: true });
    fs.writeFileSync(jsonFile, JSON.stringify(summary, null, 2));
  }
  if (keep) console.log(`작업 디렉터리 보존: ${work}`);
  else fs.rmSync(work, { recursive: true, force: true });
}
