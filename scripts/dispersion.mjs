#!/usr/bin/env node
// [진행자용] 산출물 편차 분석. 교육생 전원의 제출물을 모은 뒤 실행한다 (npm run verify 와 무관).
// 실험군(mcp)·대조군(nomcp) 각각에서 산출물이 서로 얼마나 다른지(분산도)를 재고, 같은 사람의 두 실행을 짝지어 비교한다.
//
//   npm run dispersion [-- <submissions 디렉터리>]   → submissions/dispersion.md
//
// 입력: submissions/<ID>/meta.json (도구), submissions/<ID>/<run>/project/ (collect 가 남긴 산출물 사본)
//       reference-solution/board-app 이 있으면 codegen 생성물과의 거리도 함께 낸다.
//
// 측정
//   1) 구조 거리: 프로젝트를 7개 집합(클래스, 어노테이션, 프레임워크 import, Maven 의존성, 리소스 파일, URL 매핑, 토큰 5-gram)으로
//      표현하고 집합마다 자카드 거리(1 − |A∩B|/|A∪B|)를 구한 뒤 평균한다. 패키지 경로는 루트 패키지를 떼고 비교한다.
//   2) 그룹 분산도: 그룹 안 모든 쌍의 거리 평균. mcp 와 nomcp 의 차이는 참가자 단위로 두 실행의 라벨을 뒤바꾸는 순열 검정으로 본다.
//   3) 참가자 내 비교: 각 산출물이 같은 그룹의 다른 산출물들과 떨어진 평균 거리를 구해, 같은 사람의 nomcp − mcp 를 Wilcoxon 으로 본다.
//   4) 설계 선택의 엔트로피: ID 채번·트랜잭션·영속 계층·첨부 위치·설정 방식·페이징·예외 처리 같은 갈림길마다 선택 분포와 섀넌 엔트로피.
//   5) 규모 지표의 변동 계수(CV): 파일 수, 소스 라인 수, 클래스 수.
import fs from "node:fs";
import path from "node:path";
import { KIT_DIR, SUBMISSIONS_DIR, RUNS, REFERENCE_DIR, PROJECT_NAME } from "../config/kit.config.mjs";
import { mean, sd, fmt, wilcoxon, entropy } from "./lib/stats.mjs";

const root = process.argv[2] ?? SUBMISSIONS_DIR;
const out = path.join(root, "dispersion.md");
const SRC_EXT = /\.(java|xml|properties|html|sql|yml|yaml)$/i;

// ─── 프로젝트 읽기 ─────────────────────────────────────────────────────
function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const f = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (!/^(target|\.git|node_modules)$/.test(e.name)) walk(f, acc);
    } else acc.push(f);
  }
  return acc;
}
const read = (f) => fs.readFileSync(f, "utf8");
const strip = (s) => s.replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/.*$/gm, "");
const commonPrefix = (pkgs) => {
  if (!pkgs.length) return "";
  const segs = pkgs.map((p) => p.split("."));
  const o = [];
  for (let i = 0; i < segs[0].length; i++) {
    if (segs.every((s) => s[i] === segs[0][i])) o.push(segs[0][i]);
    else break;
  }
  return o.join(".");
};

function profile(dir) {
  const files = walk(dir);
  const java = files.filter((f) => f.endsWith(".java")).map((f) => ({ f, name: path.basename(f, ".java"), src: strip(read(f)) }));
  for (const j of java) j.pkg = j.src.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1] ?? "";
  const rootPkg = java.find((j) => /@SpringBootApplication\b/.test(j.src))?.pkg ?? commonPrefix(java.map((j) => j.pkg));
  const rel = (pkg) => (pkg === rootPkg ? "" : pkg.startsWith(rootPkg + ".") ? pkg.slice(rootPkg.length + 1) : pkg);
  const all = java.map((j) => j.src).join("\n");
  const resRoot = path.join(dir, "src", "main", "resources");
  const resources = files.filter((f) => f.startsWith(resRoot) && !/[\\/]static[\\/]/.test(f)).map((f) => path.relative(resRoot, f).replace(/\\/g, "/"));
  const pom = fs.existsSync(path.join(dir, "pom.xml")) ? read(path.join(dir, "pom.xml")) : "";
  const deps = [...pom.matchAll(/<dependency>[\s\S]*?<groupId>([^<]+)<\/groupId>\s*<artifactId>([^<]+)<\/artifactId>/g)].map((m) => `${m[1].trim()}:${m[2].trim()}`);
  const sets = {
    classes: new Set(java.map((j) => `${rel(j.pkg)}.${j.name}`)),
    annotations: new Set(all.match(/@[A-Z]\w+/g) ?? []),
    imports: new Set((all.match(/^\s*import\s+(?:static\s+)?((?:org\.egovframe|org\.springframework|org\.apache\.ibatis|jakarta|org\.thymeleaf)[\w.]*)\s*;/gm) ?? []).map((l) => l.replace(/^\s*import\s+(static\s+)?/, "").replace(/\s*;$/, ""))),
    deps: new Set(deps),
    resources: new Set(resources),
    urls: new Set((all.match(/@(?:Request|Get|Post|Put|Delete)Mapping\s*\(\s*(?:value\s*=\s*)?\{?\s*"([^"]+)"/g) ?? []).map((m) => m.match(/"([^"]+)"/)[1])),
    shingles: shingles(java.map((j) => j.src)),
  };
  const srcFiles = files.filter((f) => SRC_EXT.test(f));
  const scale = {
    files: files.length,
    loc: srcFiles.reduce((s, f) => s + read(f).split("\n").length, 0),
    classes: java.length,
  };
  return { rootPkg, sets, scale, decisions: decisions(java, all, resources, rel, rootPkg) };
}
// 토큰 5-gram 집합. 문자열·숫자 리터럴은 자리표시자로 바꾸고 식별자는 그대로 둔다 (이름 선택도 편차의 일부).
function shingles(sources, k = 5) {
  const set = new Set();
  for (const src of sources) {
    const toks = src.replace(/"(?:\\.|[^"\\])*"/g, ' "S" ').replace(/\b\d+(\.\d+)?[LlFfDd]?\b/g, " N ").match(/[A-Za-z_$][\w$]*|\S/g) ?? [];
    for (let i = 0; i + k <= toks.length; i++) set.add(toks.slice(i, i + k).join(" "));
  }
  return set;
}
// 갈림길이 되는 설계 선택을 범주로 기록한다. 범주 이름은 보고서에 그대로 나온다.
function decisions(java, all, resources, rel, rootPkg) {
  const has = (re) => re.test(all);
  const boardLayer = java.filter((j) => /Board/.test(j.name) && /(Controller|Service|ServiceImpl|Mapper|DAO)$/.test(j.name));
  const boardPkg = commonPrefix(boardLayer.map((j) => j.pkg));
  const attach = java.filter((j) => !/^Board/.test(j.name) && !/Config|Test$/.test(j.name) && /File|Atch|Attach|Upload|Download|Storage/i.test(j.name));
  const attachPkgs = [...new Set(attach.map((j) => rel(j.pkg)))];
  // Board 도메인 전용 패키지가 있으면 그 하위 전체, 없으면(계층 패키지가 루트 바로 아래) Board 계층 클래스가 놓인 패키지들을 Board 영역으로 본다.
  const boardOwn = new Set(boardLayer.map((j) => rel(j.pkg)));
  const inBoard = (p) =>
    (boardPkg && boardPkg !== rootPkg ? p === rel(boardPkg) || p.startsWith(rel(boardPkg) + ".") : boardOwn.has(p)) || /(^|\.)board/i.test(p);
  const configs = java.filter((j) => /@Configuration\b/.test(j.src));
  return {
    "ID 채번": has(/IdGnrService|getNextStringId/) ? "egov-idgnr" : has(/UUID\.randomUUID/) ? "uuid" : /IDENTITY|AUTO_INCREMENT|GENERATED\s+(ALWAYS|BY DEFAULT)\s+AS/i.test(all + resources.join("\n")) ? "db-identity" : has(/currentTimeMillis|nanoTime|System\.currentTime/) ? "timestamp" : has(/MAX\s*\(\s*BOARD_ID|SELECT\s+MAX/i) ? "select-max" : "other",
    트랜잭션: has(/@Transactional\b/) && has(/TransactionInterceptor|txAdvice/) ? "annotation+aop" : has(/@Transactional\b/) ? "annotation" : has(/TransactionInterceptor|txAdvice/) ? "aop-interceptor" : "none",
    "영속 계층": has(/@Mapper\b|@EgovMapper\b/) ? "mybatis-mapper" : has(/EgovAbstractMapper|EgovComAbstractDAO|EgovAbstractDAO/) ? "egov-dao" : has(/JpaRepository|@Entity\b/) ? "jpa" : has(/JdbcTemplate/) ? "jdbc-template" : has(/SqlSession/) ? "mybatis-session" : "other",
    "첨부 위치": !attach.length ? "none" : attachPkgs.some(inBoard) ? "inside-board" : attachPkgs.some((p) => /^(cmm|cmmn|com|common|fms|file|attach)/i.test(p)) ? "common-pkg" : "separate-pkg",
    "설정 방식": configs.length >= 3 ? "java-config-split" : configs.length ? "java-config-single" : resources.some((r) => /context-.*\.xml$/.test(r)) ? "xml" : "autoconfig-only",
    페이징: has(/PaginationInfo/) ? "egov-pagination" : has(/pageIndex|Pageable/) ? "custom" : "none",
    "예외 처리": has(/SimpleMappingExceptionResolver/) && has(/@ControllerAdvice|@ExceptionHandler/) ? "resolver+advice" : has(/SimpleMappingExceptionResolver/) ? "egov-resolver" : has(/@ControllerAdvice|@ExceptionHandler/) ? "controller-advice" : has(/ErrorController/) ? "error-controller" : "none",
    "보안 설정": has(/SecurityFilterChain/) ? "filter-chain-bean" : has(/WebSecurityConfigurerAdapter/) ? "adapter" : "none",
    "루트 패키지": /^egovframework\./.test(rootPkg) ? "egovframework.*" : rootPkg ? "other" : "none",
    뷰: resources.some((r) => /templates\/.*\.html$/.test(r)) ? "thymeleaf" : resources.some((r) => /\.jsp$/.test(r)) ? "jsp" : "none",
  };
}

// ─── 거리 ───────────────────────────────────────────────────────────────
const FAMILIES = ["classes", "annotations", "imports", "deps", "resources", "urls", "shingles"];
const FAMILY_KO = { classes: "클래스", annotations: "어노테이션", imports: "프레임워크 import", deps: "의존성", resources: "리소스", urls: "URL 매핑", shingles: "토큰 5-gram" };
function jaccardDist(a, b) {
  if (!a.size && !b.size) return 0;
  let inter = 0;
  for (const x of a) if (b.has(x)) inter++;
  return 1 - inter / (a.size + b.size - inter);
}
const distance = (p, q) => {
  const per = Object.fromEntries(FAMILIES.map((f) => [f, jaccardDist(p.sets[f], q.sets[f])]));
  return { ...per, combined: mean(FAMILIES.map((f) => per[f])) };
};
const pairs = (xs) => {
  const r = [];
  for (let i = 0; i < xs.length; i++) for (let j = i + 1; j < xs.length; j++) r.push([xs[i], xs[j]]);
  return r;
};

// ─── 제출물 로드 ────────────────────────────────────────────────────────
const people = fs.existsSync(root)
  ? fs.readdirSync(root, { withFileTypes: true }).filter((e) => e.isDirectory()).map((e) => e.name).sort()
  : [];
const projects = []; // { id, run, tool, profile }
for (const id of people) {
  const meta = fs.existsSync(path.join(root, id, "meta.json")) ? JSON.parse(read(path.join(root, id, "meta.json"))) : {};
  for (const run of RUNS) {
    const dir = path.join(root, id, run, "project");
    if (fs.existsSync(path.join(dir, "pom.xml"))) projects.push({ id, run, tool: meta.tool ?? "-", profile: profile(dir) });
  }
}
if (projects.length < 2) {
  console.error(`분석할 산출물이 ${projects.length}개뿐입니다 (${root}). npm run collect 또는 gather-submissions.sh 로 제출물을 먼저 모으세요.`);
  process.exit(1);
}
const refDir = path.join(REFERENCE_DIR, PROJECT_NAME);
const reference = fs.existsSync(path.join(refDir, "pom.xml")) ? profile(refDir) : undefined;

const D = new Map(); // key "id/run|id/run" → distance
const key = (a, b) => [`${a.id}/${a.run}`, `${b.id}/${b.run}`].sort().join("|");
for (const [a, b] of pairs(projects)) D.set(key(a, b), distance(a.profile, b.profile));
const dist = (a, b) => D.get(key(a, b));

// 그룹 안 평균 쌍별 거리
const within = (group, fam = "combined") => {
  const ps = pairs(group).map(([a, b]) => dist(a, b)[fam]);
  return ps.length ? mean(ps) : undefined;
};
// 한 산출물이 같은 그룹의 다른 산출물들과 떨어진 평균 거리
const toOthers = (p, group) => {
  const others = group.filter((q) => q !== p);
  return others.length ? mean(others.map((q) => dist(p, q).combined)) : undefined;
};
const byRun = Object.fromEntries(RUNS.map((r) => [r, projects.filter((p) => p.run === r)]));
const both = people.filter((id) => RUNS.every((r) => byRun[r].some((p) => p.id === id)));

// 순열 검정: 두 실행이 모두 있는 참가자의 라벨을 무작위로 뒤바꿔 "nomcp 분산도 − mcp 분산도" 의 분포를 만든다.
function permutationTest(iters = 4000) {
  const paired = both.map((id) => RUNS.map((r) => byRun[r].find((p) => p.id === id)));
  if (paired.length < 3) return undefined;
  const stat = (groups) => within(groups[1]) - within(groups[0]);
  const observed = stat([paired.map((x) => x[0]), paired.map((x) => x[1])]);
  let extreme = 0;
  const exact = paired.length <= 12;
  const total = exact ? 2 ** paired.length : iters;
  for (let t = 0; t < total; t++) {
    const g = [[], []];
    for (let i = 0; i < paired.length; i++) {
      const flip = exact ? (t >> i) & 1 : Math.random() < 0.5;
      g[0].push(paired[i][flip]);
      g[1].push(paired[i][1 - flip]);
    }
    if (Math.abs(stat(g)) >= Math.abs(observed) - 1e-12) extreme++;
  }
  return { observed, p: extreme / total, n: paired.length, exact };
}

// ─── 보고서 ─────────────────────────────────────────────────────────────
const L = [];
const row = (...cells) => L.push(`| ${cells.join(" | ")} |`);
const sep = (n) => L.push(`|${"---|".repeat(n)}`);
const pct = (x) => (x == null ? "-" : (x * 100).toFixed(0) + "%");
L.push("# 산출물 편차 분석 (실험군 mcp / 대조군 nomcp)", "");
L.push(`제출물 ${root} · 참가자 ${people.length}명, 산출물 ${projects.length}개 (두 실행 모두 있는 참가자 ${both.length}명)`, "");
L.push("거리 = 7개 집합(클래스·어노테이션·프레임워크 import·의존성·리소스·URL 매핑·토큰 5-gram)의 자카드 거리 평균. 0 이면 같고 1 이면 공통점이 없음.", "");

L.push("## 1. 그룹 분산도 (그룹 안 평균 쌍별 거리)", "");
row("실행", "n", "종합", ...FAMILIES.map((f) => FAMILY_KO[f]));
sep(3 + FAMILIES.length);
for (const r of RUNS) row(r, byRun[r].length, fmt(within(byRun[r]), 3), ...FAMILIES.map((f) => fmt(within(byRun[r], f), 3)));
L.push("");
const perm = permutationTest();
if (perm) L.push(`nomcp − mcp 종합 분산도 차이 = **${fmt(perm.observed, 3)}**, 참가자 단위 라벨 교환 순열 검정 p = ${fmt(perm.p, 3)} (n=${perm.n}, ${perm.exact ? "전체 순열" : "무작위 4000회"})`, "");
else L.push("두 실행이 모두 있는 참가자가 3명 미만이어서 순열 검정은 생략합니다.", "");
if (reference) {
  L.push("codegen 참조 프로젝트(`npm run reference`)와의 거리 — 작을수록 도구 생성물에 가깝다:", "");
  row("실행", "n", "참조와의 거리 평균", "최소", "최대");
  sep(5);
  for (const r of RUNS) {
    const ds = byRun[r].map((p) => distance(p.profile, reference).combined);
    if (ds.length) row(r, ds.length, fmt(mean(ds), 3), fmt(Math.min(...ds), 3), fmt(Math.max(...ds), 3));
  }
  L.push("");
}
// 도구별 층
const tools = [...new Set(projects.map((p) => p.tool))];
if (tools.length > 1) {
  L.push("도구별 (같은 도구 사용자 2명 이상인 층만):", "");
  row("도구", ...RUNS.map((r) => `${r} n`), ...RUNS.map((r) => `${r} 분산도`));
  sep(1 + RUNS.length * 2);
  for (const t of tools) {
    const g = Object.fromEntries(RUNS.map((r) => [r, byRun[r].filter((p) => p.tool === t)]));
    row(t, ...RUNS.map((r) => g[r].length), ...RUNS.map((r) => (g[r].length >= 2 ? fmt(within(g[r]), 3) : "-")));
  }
  L.push("");
}

L.push("## 2. 참가자 내 비교 (같은 그룹의 다른 산출물들과 떨어진 평균 거리)", "");
row("참가자", "도구", "mcp", "nomcp", "nomcp − mcp");
sep(5);
const pr = [];
for (const id of both) {
  const m = byRun.mcp.find((p) => p.id === id);
  const n = byRun.nomcp.find((p) => p.id === id);
  const dm = toOthers(m, byRun.mcp);
  const dn = toOthers(n, byRun.nomcp);
  if (dm != null && dn != null) pr.push([dn, dm]);
  row(id, m.tool, fmt(dm, 3), fmt(dn, 3), dm != null && dn != null ? fmt(dn - dm, 3) : "-");
}
const w = wilcoxon(pr);
L.push("");
if (w) L.push(`Wilcoxon 부호 순위: nomcp 가 더 흩어진 참가자 ${w.positive}명, mcp 가 더 흩어진 참가자 ${w.negative}명, p = ${fmt(w.p, 3)} (n=${w.n}, 정규 근사)`, "");
else L.push("쌍이 부족해 Wilcoxon 검정은 생략합니다.", "");

L.push("## 3. 설계 선택의 분포와 엔트로피 (비트, 높을수록 사람마다 다른 선택)", "");
const items = Object.keys(projects[0].profile.decisions);
row("결정 항목", ...RUNS.flatMap((r) => [`${r} 분포`, `${r} H`]));
sep(1 + RUNS.length * 2);
for (const it of items) {
  const cells = [];
  for (const r of RUNS) {
    const counts = {};
    for (const p of byRun[r]) counts[p.profile.decisions[it]] = (counts[p.profile.decisions[it]] ?? 0) + 1;
    const distTxt = Object.entries(counts).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k} ${v}`).join(", ") || "-";
    cells.push(distTxt, fmt(entropy(Object.values(counts)), 2));
  }
  row(it, ...cells);
}
L.push("");

L.push("## 4. 규모 지표의 변동 계수 (CV = 표준편차 / 평균)", "");
row("실행", "n", "파일 수 평균", "CV", "소스 라인 평균", "CV", "클래스 수 평균", "CV");
sep(8);
for (const r of RUNS) {
  const g = byRun[r].map((p) => p.profile.scale);
  const cv = (k) => {
    const xs = g.map((s) => s[k]);
    const m = mean(xs);
    const s = sd(xs);
    return m && s != null ? fmt(s / m, 2) : "-";
  };
  row(r, g.length, fmt(mean(g.map((s) => s.files)), 0), cv("files"), fmt(mean(g.map((s) => s.loc)), 0), cv("loc"), fmt(mean(g.map((s) => s.classes)), 0), cv("classes"));
}
L.push("");

L.push("## 5. 쌍별 거리 행렬 (종합)", "");
for (const r of RUNS) {
  const g = byRun[r];
  if (g.length < 2) continue;
  L.push(`### ${r}`, "");
  row("", ...g.map((p) => p.id));
  sep(g.length + 1);
  for (const a of g) row(a.id, ...g.map((b) => (a === b ? "·" : fmt(dist(a, b).combined, 2))));
  L.push("");
}

L.push("## 해석 메모", "");
L.push("- mcp 산출물이 균일한 것은 같은 생성기를 썼기 때문이므로 그 자체는 당연한 결과다. 참조 프로젝트와의 거리가 곧 손으로 보완한 양이며, 그 보완 부분에 남는 편차를 본다.");
L.push("- nomcp 의 편차에는 참가자마다 다른 도구·모델의 차이가 섞인다. 도구별 층(1절)을 함께 본다.");
L.push("- 참가자가 10명 안팎이면 p 값은 참고로만 쓰고, 분산도 차이의 크기와 3절의 결정 항목별 분포로 설명한다.");
L.push("- 3절의 범주는 소스의 키워드로 자동 분류한 것이라 비정형 구현은 'other' 로 묶인다. 결과를 인용하기 전에 해당 산출물을 열어 확인한다.");
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, L.join("\n") + "\n");
const shown = path.relative(KIT_DIR, out).startsWith("..") ? out : path.relative(KIT_DIR, out);
console.log(`✅ ${shown} 작성 (산출물 ${projects.length}개, 참가자 ${people.length}명)`);
for (const r of RUNS) console.log(`   ${r}: n=${byRun[r].length}, 그룹 분산도 ${fmt(within(byRun[r]), 3)}`);
if (perm) console.log(`   nomcp − mcp = ${fmt(perm.observed, 3)}, 순열 p = ${fmt(perm.p, 3)}`);
