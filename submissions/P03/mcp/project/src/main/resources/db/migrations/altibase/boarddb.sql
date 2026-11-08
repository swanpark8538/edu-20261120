-- ============================================================================
-- altibase 운영 DB 용 DDL — 엔티티 'Board' 마이그레이션
-- ============================================================================
-- 본 파일은 spring.profiles.active 를 hsql 외의 값(altibase) 으로 변경했을 때
-- 적용할 DDL 입니다. 기본 HSQL 프로파일에서는 사용되지 않습니다.
--
-- 적용 방법:
--   - 운영 DB 에 직접 실행하거나 Flyway/Liquibase 등 마이그레이션 도구에 투입.
--   - IDS 테이블은 첫 엔티티의 마이그레이션에 포함되어 있습니다 (eGovFrame 의
--     EgovTableIdGnrService 가 사용하는 채번 테이블 — 프로젝트당 1회만 생성).
-- ============================================================================

CREATE TABLE BOARD (
	BOARD_ID	VARCHAR2(20) NOT NULL PRIMARY KEY,
	TITLE	VARCHAR2(200) NOT NULL,
	CONTENT	VARCHAR2(4000),
	WRITER	VARCHAR2(50) NOT NULL,
	ATCH_FILE_ID	VARCHAR2(20)
);

CREATE TABLE IDS (
	TABLE_NAME	VARCHAR2(16) NOT NULL PRIMARY KEY,
	NEXT_ID		NUMBER(30) NOT NULL
);
INSERT INTO IDS VALUES('BOARD', 1);
