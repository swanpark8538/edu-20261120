#!/usr/bin/env bash
# [진행자용] origin 의 submit/* 브랜치에서 submissions/<ID>/ 를 모은다. 이어서 npm run aggregate.
set -euo pipefail
cd "$(dirname "$0")/.."
git fetch --all --prune -q
for ref in $(git for-each-ref --format='%(refname:short)' 'refs/remotes/origin/submit/*'); do
  id="${ref##*/}"
  echo "▶ $ref"
  if git cat-file -e "$ref:submissions/$id" 2>/dev/null; then
    git archive "$ref" "submissions/$id" | tar -x -C .
    echo "  submissions/$id 수집"
  else
    echo "  ⚠️  submissions/$id 가 브랜치에 없음"
  fi
done
echo "✅ 완료. 다음: npm run aggregate"
