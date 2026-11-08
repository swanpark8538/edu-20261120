# 운영 DB 전환 시 SQL Mapper Portability 가이드

생성된 MyBatis mapper 는 **HSQL / MySQL / MariaDB / PostgreSQL / CUBRID 10+ / GOLDILOCKS** 환경에서
별도 수정 없이 동작합니다. **Oracle / Tibero / Altibase** 로 전환할 때는 두 가지 패턴만
수동 교체하면 됩니다.

> 본 가이드는 `egov_scaffold_project` / `egov_add_entity` / `egov_add_query_view` 도구가
> 자동 생성한 mapper 파일에 한정됩니다. 손으로 추가한 mapper 는 별도 검토하세요.

## 패치 대상 파일

```
src/main/resources/egovframework/sqlmap/{module}/mappers/Egov{Entity}_{Entity}_SQL.xml
```

엔티티당 1개 + query view 당 1개씩 있습니다.

## 비 portable 패턴 (전체)

| # | 패턴 | 발생 위치 | 영향 vendor |
|---|---|---|---|
| 1 | `LIMIT #{recordCountPerPage} OFFSET #{firstIndex}` | 각 `select*List` 의 마지막 줄 | Oracle / Tibero |
| 2 | `CONCAT('%', #{searchKeyword}, '%')` | `select*List` + `select*ListTotCnt` 의 search `<when>` 블록 | Oracle / Tibero / Altibase |

생성된 mapper 본문의 해당 위치에는 이미 portability 안내 주석이 박혀 있습니다 — `grep -n "dialect-portability"` 로 찾을 수 있습니다.

---

## Oracle 12c+ / Tibero 7+ / Altibase 7+

### ① 페이징 — LIMIT/OFFSET → OFFSET ROWS FETCH NEXT

**Before**
```xml
LIMIT #{recordCountPerPage} OFFSET #{firstIndex}
```

**After**
```xml
OFFSET #{firstIndex} ROWS FETCH NEXT #{recordCountPerPage} ROWS ONLY
```

SQL:2008 표준 문법으로 위 세 벤더 모두 지원합니다.

### ② LIKE — CONCAT() → || 연산자

**Before**
```xml
AND t.{column} LIKE CONCAT('%', #{searchKeyword}, '%')
```

**After**
```xml
AND t.{column} LIKE '%' || #{searchKeyword} || '%'
```

> ⚠️ MySQL 로 다시 돌아가는 시나리오라면 `||` 가 OR 연산자로 해석되어 깨집니다.
> 양쪽 벤더를 모두 지원해야 한다면 mapper 본문에 MyBatis `databaseId` 분기를 적용하거나,
> dialect 별로 mapper 를 별도 관리하세요.

---

## Oracle 11g 이하 — 페이징

11g 이하는 `OFFSET ROWS FETCH NEXT` 도 미지원. ROWNUM 서브쿼리로 감싸야 합니다.

**Before**
```xml
SELECT t.col1, t.col2, ...
FROM target_table t
LEFT JOIN ... ON ...
WHERE 1=1
  ...search...
ORDER BY t.pk DESC
LIMIT #{recordCountPerPage} OFFSET #{firstIndex}
```

**After**
```xml
SELECT * FROM (
    SELECT inner_t.*, ROWNUM rn FROM (
        SELECT t.col1, t.col2, ...
        FROM target_table t
        LEFT JOIN ... ON ...
        WHERE 1=1
          ...search...
        ORDER BY t.pk DESC
    ) inner_t
    WHERE ROWNUM &lt;= #{firstIndex} + #{recordCountPerPage}
)
WHERE rn &gt; #{firstIndex}
```

XML 안에서는 `<` `>` 를 그대로 쓰면 파서가 깨지므로 `&lt;` / `&gt;` 로 escape 해야 합니다.

---

## 일괄 패치 — sed 한 줄

엔티티가 많아 수동 교체가 번거로우면 다음 명령으로 일괄 처리할 수 있습니다 (실행 전 `git diff` 로 결과 확인 권장).

### Oracle 12c+ / Tibero 7+ / Altibase 7+

```bash
# 1. LIMIT/OFFSET → OFFSET ROWS FETCH NEXT
find src/main/resources/egovframework/sqlmap -name '*.xml' -exec \
  sed -i.bak -E \
    's|LIMIT #\{recordCountPerPage\} OFFSET #\{firstIndex\}|OFFSET #{firstIndex} ROWS FETCH NEXT #{recordCountPerPage} ROWS ONLY|g' \
    {} +

# 2. CONCAT('%', #{searchKeyword}, '%') → '%' || #{searchKeyword} || '%'
find src/main/resources/egovframework/sqlmap -name '*.xml' -exec \
  sed -i.bak -E \
    "s|CONCAT\('%', #\{searchKeyword\}, '%'\)|'%' || #{searchKeyword} || '%'|g" \
    {} +

# 3. .bak 백업 정리 (변경 검증 후)
find src/main/resources/egovframework/sqlmap -name '*.xml.bak' -delete
```

macOS 의 BSD sed 는 `-i.bak` 형태가 필수. GNU sed (대부분의 Linux) 는 `-i ''` 사용.

---

## 검증

패치 후 다음을 확인합니다:

```bash
# LIMIT/OFFSET 잔존 검색 — 결과 0줄 이어야 함 (Oracle/Tibero 전환 후)
grep -rn 'LIMIT #{recordCountPerPage}' src/main/resources/egovframework/sqlmap

# CONCAT 3-arg 잔존 검색 — 결과 0줄 이어야 함 (Oracle/Tibero/Altibase 전환 후)
grep -rn "CONCAT('%', #{searchKeyword}" src/main/resources/egovframework/sqlmap
```

이후 통합 테스트로 list/검색 화면이 정상 동작하는지 확인하세요.

---

## 관련 파일

- 운영 DB profile / 자격증명 주입: `src/main/resources/egovframework/egovProps/globals.properties` 상단 주석
- DataSource 설정: `src/main/java/{basePackage}/config/EgovConfigDatasource.java`
- DDL (vendor 별로 이미 분리되어 있음): `src/main/resources/db/migrations/{dialect}/{entity}db.sql`
