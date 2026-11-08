#!/usr/bin/env node
// 게시판 과제 채점기. TASK.md 의 인수 조건(HTTP 계약)을 블랙박스로 검사하고,
// 시스템 설정 항목은 소스 정적 분석과 HTTP 검사를 섞어 판정한다.
//
// 사용법
//   npm run verify -- mcp          실험군 실행(<workspace>/mcp/board-app) 채점 → submissions/<ID>/mcp/progress.jsonl 에 기록
//   npm run verify -- nomcp        대조군 실행 채점
//   npm run verify -- mcp --static 빌드·기동 없이 정적 항목만 (기록 없음)
//   node scripts/verify.mjs --project <dir> [--label L] [--log F] [--json F] [--keep] [--static]
//
// 프로젝트(board-app/pom.xml)가 아직 없어도 오류로 끝내지 않고 0점과 시각을 기록한다.
// 실습 시작 전에 한 번 실행하면 그 기록이 "시작 시각" 이 된다.
//
// 대상을 임시 디렉터리로 복사해서 빌드·기동하므로 IDE 에서 띄워 둔 앱이나 target/ 과 충돌하지 않는다.
import { spawn } from "node:child_process";
import { createServer } from "node:net";
import { randomBytes } from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { KIT_DIR, MVN, MVN_SPAWN, RUNS, checkEntry, readParticipant, projectDir as participantProjectDir, submissionDir } from "../config/kit.config.mjs";
import { MILESTONES } from "../config/milestones.mjs";

// ─── 인자 ──────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);
const opt = (name) => {
  const i = argv.indexOf(name);
  return i >= 0 ? argv[i + 1] : undefined;
};
const staticOnly = argv.includes("--static");
const keep = argv.includes("--keep");
const participant = readParticipant();
const run = argv.find((a) => RUNS.includes(a));
let projectDir = opt("--project") ? path.resolve(opt("--project")) : undefined;
if (!projectDir) {
  if (!run || !participant) {
    console.error("usage: npm run verify -- <mcp|nomcp> [--static]   또는   node scripts/verify.mjs --project <dir>");
    if (!participant) console.error(".participant.json 이 없습니다. npm run setup 을 먼저 실행하세요.");
    process.exit(1);
  }
  projectDir = participantProjectDir(participant, run);
}
const label = opt("--label") ?? (run && participant ? `${participant.id}-${run}` : path.basename(projectDir));
const logFile = opt("--log") ?? (run && participant && !staticOnly ? path.join(submissionDir(participant, run), "progress.jsonl") : undefined);
const jsonFile = opt("--json");
const CHECK_MCP = checkEntry();

const MB = 1024 * 1024;
const BUILD_TIMEOUT_MS = 8 * 60_000;
const BOOT_TIMEOUT_MS = 150_000;
const PAGE_UNIT = 10;

// ─── 결과 기록 ─────────────────────────────────────────────────────────
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
function runCmd(cmd, args, { cwd, timeoutMs, logPath, shell = false }) {
  return new Promise((resolve) => {
    const out = fs.openSync(logPath, "w");
    const p = spawn(cmd, args, { cwd, stdio: ["ignore", out, out], shell });
    // 셸을 거친 경우(Windows) p 는 cmd.exe 이므로 자식까지 함께 끝낸다.
    const kill = () => (shell && process.platform === "win32" ? spawn("taskkill", ["/pid", String(p.pid), "/T", "/F"]) : p.kill("SIGKILL"));
    const t = setTimeout(kill, timeoutMs);
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
  async follow(r) {
    const loc = r.headers.get("location");
    if (r.status >= 300 && r.status < 400 && loc) return this.req("GET", loc);
    return r;
  }
  get(url) {
    return this.req("GET", url);
  }
  async postMultipart(url, fields, files = [], { withCsrf = true } = {}) {
    const { body, contentType } = buildMultipart(withCsrf ? { ...fields, _csrf: this.csrf } : fields, files);
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
function rowId(row) {
  const r = decodeHtml(row);
  const m =
    r.match(/on\w+\s*=\s*"[^"]*?\w+\(\s*['"]?([A-Za-z0-9_\-]+)['"]?\s*[,)]/) ??
    r.match(/boardId=([A-Za-z0-9_\-]+)/) ??
    r.match(/name="boardId"[^>]*value="([A-Za-z0-9_\-]+)"/) ??
    r.match(/value="([A-Za-z0-9_\-]+)"[^>]*name="boardId"/) ??
    r.match(/<td[^>]*>(?:\s*<[^>]+>)*\s*([A-Za-z0-9_\-]+)\s*</);
  return m?.[1];
}
const rows = (html) => html.split(/<tr[\s>]/i).slice(1);
// 화면 전체에서 게시글 ID 로 보이는 값들: 행 패턴 + boardId=… 링크 + hidden boardId. 표가 아닌 화면에서도 동작한다.
const ID_PATTERNS = [/boardId=([A-Za-z0-9_\-]+)/g, /name="boardId"[^>]*value="([A-Za-z0-9_\-]+)"/g, /value="([A-Za-z0-9_\-]+)"[^>]*name="boardId"/g];
function allIds(html) {
  const set = new Set(rows(html).map(rowId).filter(Boolean));
  const r = decodeHtml(html);
  for (const re of ID_PATTERNS) for (const m of r.matchAll(re)) set.add(m[1]);
  return set;
}
// 제목이 든 행에서 ID 를 찾고, 없으면 등록 전 화면(before)에 없던 ID 가 하나뿐일 때 그것을 쓴다.
function findRowId(html, title, before) {
  if (!html.includes(title)) return undefined;
  for (const row of rows(html)) if (row.includes(title)) { const id = rowId(row); if (id) return id; }
  if (before) {
    const fresh = [...allIds(html)].filter((x) => !before.has(x));
    if (fresh.length === 1) return fresh[0];
  }
  return undefined;
}
function findDownloadLinks(html) {
  const links = new Set();
  for (const m of html.matchAll(/href="([^"]+)"/g)) {
    const u = decodeHtml(m[1]);
    if (/FileDown|atchFileId=|fileSn=|download|attach/i.test(u)) links.add(u);
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
  const base = (f) => path.basename(f);

  // C1 Config 클래스: @Configuration 클래스로 DataSource·MyBatis(SqlSession)·트랜잭션 설정이 정의되어 있는가
  const configs = java.filter((f) => /@Configuration\b/.test(stripComments(read(f))));
  const cfgSrc = configs.map((f) => stripComments(read(f))).join("\n");
  const need = [
    ["DataSource", /\bDataSource\b/],
    ["SqlSessionFactory", /SqlSessionFactory/],
    ["TransactionManager", /TransactionManager/],
  ].filter(([, re]) => !re.test(cfgSrc)).map(([n]) => n);
  const c1 = {
    pass: configs.length >= 3 && need.length === 0,
    detail: configs.length
      ? `@Configuration ${configs.length}개 (${configs.slice(0, 6).map(base).join(", ")}${configs.length > 6 ? " …" : ""})${need.length ? ` — 누락: ${need.join(", ")}` : ""}`
      : "@Configuration 클래스가 없음",
  };

  // C2 트랜잭션
  const impl = boardJava.find((f) => /Board\w*ServiceImpl\.java$/.test(f)) ?? java.find((f) => /Board\w*ServiceImpl\.java$/.test(f));
  let c2;
  if (!impl) c2 = { pass: false, detail: "Board ServiceImpl 클래스를 찾지 못함" };
  else {
    const src = stripComments(read(impl));
    const pkg = src.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1] ?? "";
    const fqn = `${pkg}.${path.basename(impl, ".java")}`;
    if (/@Transactional\b/.test(src)) c2 = { pass: true, detail: `${base(impl)} 에 @Transactional` };
    else {
      const txFiles = java.filter((f) => /TransactionInterceptor|txAdvice|EnableTransactionManagement/.test(stripComments(read(f))));
      const patterns = [];
      for (const f of txFiles)
        for (const m of stripComments(read(f)).matchAll(/execution\s*\(\s*\*?\s*([\w.*]+)\s*\(/g)) patterns.push({ file: base(f), expr: m[1] });
      const hit = patterns.find((p) => aspectjMatches(p.expr, `${fqn}.insertBoard`));
      c2 = hit
        ? { pass: true, detail: `${hit.file} 의 포인트컷 ${hit.expr} 이 ${base(impl)} 을 포함` }
        : { pass: false, detail: patterns.length ? `포인트컷 ${patterns.map((p) => p.expr).join(" / ")} 이 ${fqn} 을 포함하지 않음 (@Transactional 도 없음)` : "트랜잭션 포인트컷을 찾지 못함" };
    }
  }

  // C3(정적) 표준 ID Generation 서비스 사용
  const idgnr = boardJava.filter((f) => /IdGnrService|EgovTableIdGnrServiceImpl|getNextStringId/.test(stripComments(read(f))));
  const c3s = { pass: idgnr.length > 0, detail: idgnr.length ? `${idgnr.map(base).join(", ")} 에서 EgovIdGnrService 사용` : "Board 소스에서 EgovIdGnrService(getNextStringId) 사용 흔적 없음" };

  // C4(정적) 입력 검증 어노테이션
  const vo = boardJava.filter((f) => /@(Valid|NotBlank|NotNull|NotEmpty|Size|EgovNullCheck)\b/.test(stripComments(read(f))));
  const c4s = { pass: vo.length > 0, detail: vo.length ? "" : "Board 소스에 검증 어노테이션(@Valid/@NotBlank/@EgovNullCheck 등)이 없음" };

  // U3(정적) KRDS 자산
  const krdsFiles = walk(resRoot, (f) => /krds/i.test(path.relative(resRoot, f)));
  const boardLayerFiles = java.filter((f) => /Board\w*(Controller|ServiceImpl|DAO|Mapper)\.java$/.test(f)).map((f) => path.relative(proj, f));
  const p1 = packageLayout(java, configs);
  return { c1, c2, c3s, c4s, p1, krdsFiles: krdsFiles.length, boardLayerFiles };
}
// P1 패키지 구성: Board 도메인 · 첨부 기능 · 설정이 서로 다른 패키지에 있는가 (도메인 단위 패키지 분리)
//  - 루트 = @SpringBootApplication 클래스의 패키지. 도메인 패키지는 루트보다 깊어야 한다.
//  - Board 도메인 패키지 = Board 계층 클래스(Controller/Service/ServiceImpl/Mapper/DAO)들의 공통 상위 패키지.
//    루트와 같으면(계층 패키지가 루트 바로 아래) 도메인 패키지가 없는 것으로 본다.
//  - 첨부 기능 클래스(이름에 File/Atch/Attach/Upload/Download/Storage, Board* 와 *Config* 제외)는 Board 도메인 패키지 밖에 있어야 하고,
//    Board 계층 클래스 안에 파일 저장·다운로드 처리(transferTo, Files.copy, Content-Disposition 등)가 직접 들어 있으면 안 된다.
//  - @Configuration 클래스는 Board 도메인 패키지 밖에 있어야 한다.
//  - 한 패키지에 @Controller 와 @Service/@Repository/@Mapper 구현이 함께 있으면 계층 분리가 안 된 것으로 본다.
function packageLayout(java, configs) {
  const info = java.map((f) => {
    const src = stripComments(read(f));
    return { f, name: path.basename(f, ".java"), pkg: src.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1] ?? "", src };
  });
  if (!info.length) return { pass: false, detail: "자바 소스가 없음" };
  const commonPrefix = (pkgs) => {
    if (!pkgs.length) return "";
    const segs = pkgs.map((p) => p.split("."));
    const out = [];
    for (let i = 0; i < segs[0].length; i++) {
      if (segs.every((s) => s[i] === segs[0][i])) out.push(segs[0][i]);
      else break;
    }
    return out.join(".");
  };
  const under = (pkg, base) => base !== "" && (pkg === base || pkg.startsWith(base + "."));
  const root = info.find((i) => /@SpringBootApplication\b/.test(i.src))?.pkg ?? commonPrefix(info.map((i) => i.pkg));
  const rel = (pkg) => (under(pkg, root) ? pkg.slice(root.length).replace(/^\./, "") || "(루트)" : pkg);
  const problems = [];

  const boardLayer = info.filter((i) => /Board/.test(i.name) && /(Controller|Service|ServiceImpl|Mapper|DAO)$/.test(i.name));
  const boardPkg = commonPrefix(boardLayer.map((i) => i.pkg));
  const hasDomainPkg = boardPkg !== "" && boardPkg.split(".").length > root.split(".").length;
  // Board 도메인으로 간주하는 패키지: 전용 패키지가 있으면 그 하위 전체, 없으면 Board 계층 클래스가 놓인 패키지들. 루트 아래 'board' 세그먼트도 포함.
  const boardPkgs = hasDomainPkg ? [boardPkg] : [...new Set(boardLayer.map((i) => i.pkg))];
  const inBoard = (pkg) => boardPkgs.some((b) => under(pkg, b)) || rel(pkg).split(".").some((seg) => /^board/i.test(seg));
  if (!boardLayer.length) problems.push("Board 계층 클래스(Controller/Service/Mapper)를 찾지 못함");
  else if (!hasDomainPkg) problems.push(`Board 도메인 전용 패키지가 없음 (계층 패키지가 루트 ${root} 바로 아래: ${[...new Set(boardLayer.map((i) => rel(i.pkg)))].join(", ")})`);

  const attach = info.filter((i) => !/^Board/.test(i.name) && !/Config|Test$/.test(i.name) && /File|Atch|Attach|Upload|Download|Storage/i.test(i.name));
  const attachInBoard = attach.filter((i) => inBoard(i.pkg));
  const inlineIo = boardLayer.filter((i) => /\b(transferTo|Files\.(copy|write|createDirectories)|FileOutputStream|FileInputStream|FileSystemResource|Content-Disposition|APPLICATION_OCTET_STREAM)\b/.test(i.src));
  if (!attach.length && !inlineIo.length) problems.push("첨부 기능 클래스를 찾지 못함 (분리 여부를 판단할 수 없음)");
  if (attachInBoard.length) problems.push(`첨부 기능 클래스가 Board 도메인 패키지 안에 있음: ${attachInBoard.map((i) => `${rel(i.pkg)}.${i.name}`).join(", ")}`);
  if (inlineIo.length) problems.push(`첨부 저장·다운로드 처리가 Board 클래스 안에 구현됨: ${inlineIo.map((i) => i.name).join(", ")}`);

  const cfg = info.filter((i) => configs.includes(i.f));
  const cfgInBoard = cfg.filter((i) => inBoard(i.pkg));
  if (cfgInBoard.length) problems.push(`설정 클래스가 Board 도메인 패키지 안에 있음: ${cfgInBoard.map((i) => `${rel(i.pkg)}.${i.name}`).join(", ")}`);

  const byPkg = new Map();
  for (const i of info) {
    const kind = /@(Controller|RestController)\b/.test(i.src) ? "web" : /@(Service|Repository|Mapper)\b/.test(i.src) ? "svc" : null;
    if (kind) byPkg.set(i.pkg, new Set([...(byPkg.get(i.pkg) ?? []), kind]));
  }
  const mixed = [...byPkg].filter(([, k]) => k.size > 1).map(([p]) => rel(p));
  if (mixed.length) problems.push(`컨트롤러와 서비스 구현이 같은 패키지에 있음: ${mixed.join(", ")}`);

  const pkgs = (xs) => [...new Set(xs.map((i) => rel(i.pkg)))].join(", ") || "-";
  return {
    pass: problems.length === 0,
    detail: problems.length ? problems.join("; ") : `루트 ${root} · Board ${rel(boardPkg)} · 첨부 ${pkgs(attach)} · 설정 ${pkgs(cfg)}`,
  };
}
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
  return { pass: problems.length === 0, detail: `적용률 ${Number.isNaN(rate) ? "?" : rate + "%"} (검사 ${total}개, Board 계층 ${boardLayerFiles.length}개)${problems.length ? " — " + problems.join("; ") : ""}`, rate, total, failed };
}

// ─── 본 검사 ───────────────────────────────────────────────────────────
const work = fs.mkdtempSync(path.join(os.tmpdir(), "egov-verify-"));
let app;
let compliance;
const hasProject = fs.existsSync(path.join(projectDir, "pom.xml"));

async function main() {
  console.log(`▶ 채점 대상: ${projectDir}${run ? ` (${run})` : ""}`);
  if (!hasProject) {
    console.log("   board-app/pom.xml 이 아직 없습니다. 모든 항목을 0 으로 기록합니다 (시작 시각 기록용).");
    for (const [id] of MILESTONES) skip(id, "프로젝트 없음");
    return;
  }
  const proj = path.join(work, "project");
  fs.cpSync(projectDir, proj, { recursive: true, filter: (src) => !/[\\/](target|\.git|node_modules)$/.test(src) });
  const st = staticChecks(proj);

  // B1 빌드
  const buildLog = path.join(work, "build.log");
  let jar;
  if (staticOnly) record("B1", false, "--static: 빌드 생략");
  else {
    const code = await runCmd(MVN, ["-q", "-B", "-DskipTests", "package"], { cwd: proj, timeoutMs: BUILD_TIMEOUT_MS, logPath: buildLog, ...MVN_SPAWN });
    if (code === 0) {
      const t = path.join(proj, "target");
      jar = fs.existsSync(t) ? fs.readdirSync(t).find((f) => f.endsWith(".jar") && !f.endsWith(".original")) : undefined;
    }
    record("B1", Boolean(jar), jar ? "" : `mvn 종료 코드 ${code}\n${tail(buildLog)}`);
  }

  // B2 기동
  let s;
  let listRes;
  if (jar) {
    const port = await freePort();
    const appLog = path.join(work, "app.log");
    const store = path.join(work, "filestore");
    fs.mkdirSync(store);
    const out = fs.openSync(appLog, "w");
    app = spawn("java", ["-jar", path.join(proj, "target", jar), `--server.port=${port}`, `--Globals.fileStorePath=${store}${path.sep}`], { cwd: proj, stdio: ["ignore", out, out], shell: false });
    let exited = false;
    app.on("exit", () => (exited = true));
    s = new Session(`http://127.0.0.1:${port}`);
    const deadline = Date.now() + BOOT_TIMEOUT_MS;
    while (Date.now() < deadline && !exited) {
      try {
        listRes = await s.get("/egovBoardList.do");
        break;
      } catch {}
      await sleep(1500);
    }
    const ok = listRes?.status === 200;
    record("B2", ok, ok ? "" : exited ? `앱이 종료됨\n${tail(appLog)}` : `응답 상태 ${listRes?.status ?? "없음"}`);
    if (ok && !s.takeCsrf(listRes.text)) {
      // 목록 화면에 토큰이 없으면 루트 → 등록 화면(GET, 토큰 없는 POST) → XSRF-TOKEN 쿠키 순으로 찾는다.
      const tries = [() => s.get("/"), () => s.get("/addBoardView.do"), () => s.postMultipart("/addBoardView.do", {}, [], { withCsrf: false })];
      for (const t of tries) {
        try {
          if (s.takeCsrf((await t()).text)) break;
        } catch {}
      }
      if (!s.csrf && s.cookies.get("XSRF-TOKEN")) s.csrf = decodeURIComponent(s.cookies.get("XSRF-TOKEN"));
      if (!s.csrf) console.log("   ⚠️  목록·등록 화면과 쿠키 어디에서도 CSRF 토큰(_csrf)을 찾지 못했습니다. CSRF 보호가 켜져 있으면 POST 요청이 거부됩니다.");
    }
    if (!ok) s = undefined;
  } else skip("B2", staticOnly ? "--static: 기동 생략" : "B1 실패로 건너뜀");

  const why = staticOnly ? "--static: 생략" : "B2 실패로 건너뜀";
  const stamp = Date.now().toString(36);
  const list = async (qs = "") => (await s.get(`/egovBoardList.do${qs}`)).text;
  const emptyFile = { field: "file_1", filename: "", data: Buffer.alloc(0) };
  const add = (title, files = [emptyFile], extra = {}, o = {}) =>
    s.postMultipart("/addBoard.do", { title, content: `본문 ${title}`, writer: "verify", ...extra }, files, o);
  const updateView = (id) => s.postMultipart("/updateBoardView.do", { boardId: id });
  const del = (id) => s.postMultipart("/deleteBoard.do", { boardId: id });

  // ── 기본 기능: CRUD ───────────────────────────────────────────────
  if (s) {
    const t1 = `verify-crud-${stamp}`;
    let id;
    let v;
    try {
      const before = allIds(await list());
      const r = await add(t1);
      id = findRowId(await list(), t1, before);
      record("F1", r.status < 400 && Boolean(id), r.status >= 400 ? `등록 응답 ${r.status}` : id ? "" : "등록 후 목록에서 게시글(ID)을 찾지 못함");
    } catch (e) {
      record("F1", false, `요청 오류: ${e.message}`);
    }
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
        const u = await s.postMultipart("/updateBoard.do", { boardId: id, title: t2, content: "수정됨", writer: "verify", atchFileId: pick(v.text, "atchFileId") ?? "" }, [emptyFile]);
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
  } else for (const m of ["F1", "F2", "F3", "F4"]) skip(m, why);

  // ── 기본 기능: 첨부 ───────────────────────────────────────────────
  if (s) {
    const t = `verify-attach-${stamp}`;
    const data = randomBytes(2 * MB);
    let links = [];
    try {
      const before = allIds(await list());
      const r = await add(t, [{ field: "file_1", filename: "verify-2mb.pdf", contentType: "application/pdf", data }]);
      const id = findRowId(await list(), t, before);
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
  } else for (const m of ["F5", "F6"]) skip(m, why);

  // ── 사용자 편의 ───────────────────────────────────────────────────
  let titles = [];
  let needle = "";
  if (s && results.F1.pass) {
    needle = `needle${randomBytes(3).toString("hex")}`;
    titles = Array.from({ length: PAGE_UNIT + 2 }, (_, i) => `pg-${stamp}-${String(i + 1).padStart(2, "0")}${i === 6 ? "-" + needle : ""}`);
    try {
      for (const t of titles) await add(t);
      const p1 = await list();
      const p2 = await list("?pageIndex=2");
      const in1 = titles.filter((t) => p1.includes(t));
      const in2 = titles.filter((t) => p2.includes(t));
      const union = new Set([...in1, ...in2]);
      const problems = [];
      if (in1.length >= titles.length) problems.push(`1페이지에 ${titles.length}건이 모두 표시됨 (분할 안 됨)`);
      if (union.size < titles.length) problems.push(`1·2페이지를 합쳐도 ${union.size}/${titles.length}건만 표시`);
      if (!/pageIndex|pagination|paging/i.test(p1)) problems.push("페이지 이동 요소(pageIndex)가 없음");
      record("U1", problems.length === 0, problems.length ? problems.join("; ") : `1페이지 ${in1.length}건 + 2페이지 ${in2.length}건`);
    } catch (e) {
      record("U1", false, `요청 오류: ${e.message}`);
    }
    try {
      let hit;
      for (const cond of ["0", "1", "2", ""]) {
        const qs = `?searchKeyword=${needle}${cond === "" ? "" : `&searchCondition=${cond}`}`;
        const html = await list(qs);
        if (html.includes(titles[6]) && !html.includes(titles[0])) {
          hit = qs;
          break;
        }
      }
      const none = await list(`?searchCondition=0&searchKeyword=zz${needle}zz`);
      record("U2", Boolean(hit) && !looksLikeCrash(none), hit ?? "searchKeyword 로 요청해도 목록이 필터링되지 않음 (searchCondition 0/1/2 모두 시도)");
    } catch (e) {
      record("U2", false, `요청 오류: ${e.message}`);
    }
  } else {
    skip("U1", s ? "F1 실패로 건너뜀" : why);
    skip("U2", s ? "F1 실패로 건너뜀" : why);
  }
  // U3 KRDS: 화면 HTML 이 krds 클래스/CSS 를 쓰고, 프로젝트에 KRDS 자산이 있다
  {
    const problems = [];
    if (s) {
      const html = listRes.text;
      const classHits = (html.match(/class="[^"]*\bkrds-[\w-]+/g) ?? []).length;
      const cssHit = /<link[^>]+href="[^"]*krds[^"]*"/i.test(html) || /<link[^>]+href="[^"]*egovframe\.css[^"]*"/i.test(html);
      if (classHits < 3) problems.push(`목록 화면의 krds- 클래스 ${classHits}개 (3개 이상 필요)`);
      if (!cssHit) problems.push("KRDS 스타일시트(krds*.css 또는 egovframe.css) 링크 없음");
    } else problems.push(why);
    if (!st.krdsFiles) problems.push("프로젝트 리소스에 krds 자산 파일 없음");
    record("U3", problems.length === 0, problems.length ? problems.join("; ") : `krds 자산 ${st.krdsFiles}개`);
  }

  // ── 시스템 설정 ───────────────────────────────────────────────────
  record("C1", st.c1.pass, st.c1.detail);
  record("C2", st.c2.pass, st.c2.detail);
  {
    const problems = [];
    if (!st.c3s.pass) problems.push(st.c3s.detail);
    if (s) {
      try {
        const form = (await s.postMultipart("/addBoardView.do", {})).text;
        const editable = [...form.matchAll(/<input[^>]*name="boardId"[^>]*>/g)].map((m) => m[0]).filter((i) => !/type="hidden"|readonly|disabled/.test(i));
        if (editable.length) problems.push("등록 화면에 사용자가 입력하는 boardId 입력란이 있음");
      } catch (e) {
        problems.push(`등록 화면 요청 오류: ${e.message}`);
      }
      if (!results.F1.pass) problems.push("등록(F1)이 실패해 채번 결과를 확인할 수 없음");
    } else problems.push(why);
    record("C3", problems.length === 0, problems.length ? problems.join("; ") : st.c3s.detail);
  }
  {
    const problems = [];
    if (!st.c4s.pass) problems.push(`[검증] ${st.c4s.detail}`);
    if (s) {
      try {
        const before = allIds(await list());
        const r = await s.follow(await add("", [emptyFile], { content: `invalid-${stamp}`, writer: "verify" }));
        const after = allIds(await list());
        if (isServerError(r) || looksLikeCrash(r.text)) problems.push(`[검증] 빈 제목 등록 시 ${r.status} 오류 화면`);
        else if ([...after].some((x) => !before.has(x))) problems.push("[검증] 빈 제목인데 게시글이 등록됨 (@Valid 검증 없음)");
        const bad = await s.follow(await updateView(`NOPE-${stamp}`));
        if (looksLikeCrash(bad.text)) problems.push(`[예외] 없는 ID 조회 시 처리되지 않은 오류 화면 (${bad.status})`);
      } catch (e) {
        problems.push(`요청 오류: ${e.message}`);
      }
    } else problems.push(why);
    record("C4", problems.length === 0, problems.join("; "));
  }

  // ── 패키지 구성 (정적) ─────────────────────────────────────────────
  record("P1", st.p1.pass, st.p1.detail);

  // ── 보안 ──────────────────────────────────────────────────────────
  if (s && results.F5.pass) {
    const problems = [];
    try {
      const te = `verify-exe-${stamp}`;
      const exe = Buffer.concat([Buffer.from("MZ"), randomBytes(1024)]);
      const beforeExe = allIds(await list());
      const re = await add(te, [{ field: "file_1", filename: "evil.exe", data: exe }]);
      if (isServerError(re)) problems.push(`exe 업로드 시 ${re.status} 오류 화면`);
      const ide = findRowId(await list(), te, beforeExe);
      if (ide) for (const l of findDownloadLinks((await updateView(ide)).text)) if ((await s.get(l)).buf.equals(exe)) problems.push("exe 파일이 저장되어 다운로드됨");
      const tb = `verify-big-${stamp}`;
      const rb = await add(tb, [{ field: "file_1", filename: "big.pdf", contentType: "application/pdf", data: randomBytes(11 * MB) }]);
      if (isServerError(rb)) problems.push(`10MB 초과 업로드 시 ${rb.status} 오류 화면`);
      if ((await list()).includes(tb)) problems.push("10MB 초과 파일의 게시글이 등록됨");
    } catch (e) {
      problems.push(`요청 오류: ${e.message}`);
    }
    record("S1", problems.length === 0, problems.join(", "));
  } else skip("S1", s ? "F5 실패로 건너뜀" : why);
  if (s && results.F1.pass) {
    const problems = [];
    try {
      // XSS: 스크립트가 섞인 제목이 목록·수정 화면에서 이스케이프되어야 한다.
      const payload = `<svg/onload=alert(1)>xss-${stamp}`;
      const beforeXss = allIds(await list());
      await add(payload);
      const html = await list();
      const id = findRowId(html, `xss-${stamp}`, beforeXss);
      const raw = (h) => /<svg\/onload=alert\(1\)>/i.test(h);
      if (raw(html)) problems.push("[XSS] 목록 화면에 스크립트 제목이 이스케이프되지 않음");
      if (id && raw((await updateView(id)).text)) problems.push("[XSS] 수정 화면에 스크립트 제목이 이스케이프되지 않음");
      // CSRF: 토큰 없는 POST 는 거부되어야 한다 (게시글이 생기지 않음).
      const tc = `csrf-${stamp}`;
      const before = allIds(await list());
      const rc = await add(tc, [emptyFile], {}, { withCsrf: false });
      const after = allIds(await list());
      if ([...after].some((x) => !before.has(x)) || (await list()).includes(tc)) problems.push(`[CSRF] 토큰 없는 등록 요청이 처리됨 (응답 ${rc.status})`);
      // 보안 응답 헤더
      const h = listRes.headers;
      if (!/nosniff/i.test(h.get("x-content-type-options") ?? "")) problems.push("[헤더] X-Content-Type-Options: nosniff 없음");
      if (!h.get("x-frame-options") && !/frame-ancestors/i.test(h.get("content-security-policy") ?? "")) problems.push("[헤더] X-Frame-Options(또는 CSP frame-ancestors) 없음");
    } catch (e) {
      problems.push(`요청 오류: ${e.message}`);
    }
    record("S2", problems.length === 0, problems.join("; "));
  } else skip("S2", s ? "F1 실패로 건너뜀" : why);

  // ── 표준 준수율 ───────────────────────────────────────────────────
  try {
    compliance = await complianceCheck(proj, st.boardLayerFiles);
    record("R1", compliance.pass, compliance.detail);
  } catch (e) {
    record("R1", false, `검사 실행 실패: ${e.message}`);
  }
}

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
    run: run ?? null,
    project: projectDir,
    projectExists: hasProject,
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
    const rel = path.relative(KIT_DIR, logFile);
    console.log(`기록: ${rel.startsWith("..") ? logFile : rel}`);
  }
  if (jsonFile) {
    fs.mkdirSync(path.dirname(jsonFile), { recursive: true });
    fs.writeFileSync(jsonFile, JSON.stringify(summary, null, 2));
  }
  if (keep) console.log(`작업 디렉터리 보존: ${work}`);
  else fs.rmSync(work, { recursive: true, force: true });
}
