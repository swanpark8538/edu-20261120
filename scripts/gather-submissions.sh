#!/usr/bin/env bash
# [진행자용] origin 의 submit/* 브랜치에서 results/human/<ID>/ 를 모으고 변경 규모를 다시 계산한다.
# 사용법: bash scripts/gather-submissions.sh   → 이어서 npm run aggregate
set -euo pipefail
cd "$(dirname "$0")/.."
git fetch --all --prune -q
for ref in $(git for-each-ref --format='%(refname:short)' 'refs/remotes/origin/submit/*'); do
  id="${ref##*/}"
  echo "▶ $ref"
  mkdir -p "results/human/$id"
  git archive "$ref" "results/human/$id" 2>/dev/null | tar -x -C . || echo "  ⚠️  results/human/$id 가 브랜치에 없음"
  git diff --shortstat main "$ref" -- board-app > "results/human/$id/diffstat.txt" || true
  cat "results/human/$id/diffstat.txt"
done
echo "✅ 완료. 다음: npm run aggregate"
