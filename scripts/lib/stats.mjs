// 집계·분석 스크립트가 공유하는 작은 통계 유틸.
export const median = (xs) => {
  const a = xs.filter((x) => x != null && !Number.isNaN(x)).sort((p, q) => p - q);
  if (!a.length) return undefined;
  const m = Math.floor(a.length / 2);
  return a.length % 2 ? a[m] : (a[m - 1] + a[m]) / 2;
};
export const mean = (xs) => {
  const a = xs.filter((x) => x != null && !Number.isNaN(x));
  return a.length ? a.reduce((s, x) => s + x, 0) / a.length : undefined;
};
export const sd = (xs) => {
  const a = xs.filter((x) => x != null && !Number.isNaN(x));
  if (a.length < 2) return undefined;
  const m = mean(a);
  return Math.sqrt(a.reduce((s, x) => s + (x - m) ** 2, 0) / (a.length - 1));
};
export const fmt = (x, d = 1) => (x == null || Number.isNaN(x) ? "-" : x.toFixed(d));

// Wilcoxon 부호 순위 검정 (정규 근사). 쌍 표본이 작으므로 p 는 참고용.
export function wilcoxon(pairs) {
  const d = pairs.map(([a, b]) => a - b).filter((x) => x !== 0);
  if (d.length < 2) return undefined;
  const ranked = d.map((x) => ({ x, r: 0 })).sort((p, q) => Math.abs(p.x) - Math.abs(q.x));
  for (let i = 0; i < ranked.length; ) {
    let j = i;
    while (j + 1 < ranked.length && Math.abs(ranked[j + 1].x) === Math.abs(ranked[i].x)) j++;
    for (let k = i; k <= j; k++) ranked[k].r = (i + j) / 2 + 1;
    i = j + 1;
  }
  const wPlus = ranked.filter((v) => v.x > 0).reduce((s, v) => s + v.r, 0);
  const n = d.length;
  const mu = (n * (n + 1)) / 4;
  const sigma = Math.sqrt((n * (n + 1) * (2 * n + 1)) / 24);
  const z = (wPlus - mu) / sigma;
  const p = 2 * (1 - normCdf(Math.abs(z)));
  return { n, wPlus, p, positive: d.filter((x) => x > 0).length, negative: d.filter((x) => x < 0).length };
}
export function normCdf(x) {
  const t = 1 / (1 + 0.2316419 * x);
  const dd = 0.3989423 * Math.exp((-x * x) / 2);
  return 1 - dd * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))));
}
// 섀넌 엔트로피(비트). counts: 범주별 개수.
export function entropy(counts) {
  const n = counts.reduce((s, c) => s + c, 0);
  if (!n) return undefined;
  return -counts.filter((c) => c > 0).reduce((s, c) => s + (c / n) * Math.log2(c / n), 0);
}
