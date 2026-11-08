-- ============================================================================
-- mysql 운영 DB 용 DDL — 엔티티 'Board' 마이그레이션
-- ============================================================================
-- 본 파일은 spring.profiles.active 를 hsql 외의 값(mysql) 으로 변경했을 때
-- 적용할 DDL 입니다. 기본 HSQL 프로파일에서는 사용되지 않습니다.
--
-- 적용 방법:
--   - 운영 DB 에 직접 실행하거나 Flyway/Liquibase 등 마이그레이션 도구에 투입.
--   - IDS 테이블은 첫 엔티티의 마이그레이션에 포함되어 있습니다 (eGovFrame 의
--     EgovTableIdGnrService 가 사용하는 채번 테이블 — 프로젝트당 1회만 생성).
-- ============================================================================

CREATE TABLE BOARD (
	BOARD_ID	VARCHAR(20) NOT NULL PRIMARY KEY,
	TITLE	VARCHAR(200) NOT NULL,
	CONTENT	VARCHAR(4000),
	WRITER	VARCHAR(50) NOT NULL,
	ATCH_FILE_ID CHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IDS (
	TABLE_NAME	VARCHAR(16) NOT NULL PRIMARY KEY,
	NEXT_ID		DECIMAL(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO IDS VALUES('BOARD', 1);
