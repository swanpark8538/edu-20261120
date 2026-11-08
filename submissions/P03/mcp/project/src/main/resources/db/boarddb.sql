CREATE MEMORY TABLE BOARD(
	BOARD_ID VARCHAR(20) NOT NULL PRIMARY KEY,
	TITLE VARCHAR(200) NOT NULL,
	CONTENT VARCHAR(4000),
	WRITER VARCHAR(50) NOT NULL,
	ATCH_FILE_ID VARCHAR(20)
);

CREATE MEMORY TABLE IDS(
	TABLE_NAME VARCHAR(16) NOT NULL PRIMARY KEY,
	NEXT_ID DECIMAL(30) NOT NULL
);

INSERT INTO IDS VALUES('BOARD', 1);

-- ============================================================
-- 첨부파일 인프라 DDL — HSQL 임베디드 시드용
-- ============================================================
-- COMTNFILE / COMTNFILEDETAIL / COMTECOPSEQ + FILE_ID 시퀀스 row.
--
-- 호출자: egov_add_attach_file_infrastructure 도구.
-- EgovConfigDatasource 의 hsql 분기가 .addScript() 로 로드하는 시드 파일
-- (`src/main/resources/db/<entity>db.sql`) 끝에 append 되거나, 시드 후보를
-- 결정할 수 없는 경우 `src/main/resources/db/attach-file.sql` 별도 파일로
-- 떨어진다. 별도 파일 케이스에서는 사용자가 직접 시드에 머지해야 임베디드
-- HSQL 부팅 시 자동 반영된다.
--
-- 운영 DB 적용 DDL 은 별도 — src/main/resources/db/migrations/<dialect>/attach-file.sql
-- (oracle/tibero/altibase/cubrid/goldilocks/mysql/postgres) 참고.

CREATE MEMORY TABLE COMTNFILE (
	ATCH_FILE_ID  CHAR(20)        NOT NULL,
	CREAT_DT      TIMESTAMP       NOT NULL,
	USE_AT        CHAR(1),
	CONSTRAINT COMTNFILE_PK PRIMARY KEY (ATCH_FILE_ID)
);

CREATE MEMORY TABLE COMTNFILEDETAIL (
	ATCH_FILE_ID     CHAR(20)        NOT NULL,
	FILE_SN          DECIMAL(10)     NOT NULL,
	FILE_STRE_COURS  VARCHAR(2000)   NOT NULL,
	STRE_FILE_NM     VARCHAR(255)    NOT NULL,
	ORIGNL_FILE_NM   VARCHAR(255),
	FILE_EXTSN       VARCHAR(20)     NOT NULL,
	FILE_CN          LONGVARCHAR,
	FILE_SIZE        DECIMAL(8),
	CONSTRAINT COMTNFILEDETAIL_PK PRIMARY KEY (ATCH_FILE_ID, FILE_SN),
	CONSTRAINT COMTNFILEDETAIL_FK1 FOREIGN KEY (ATCH_FILE_ID) REFERENCES COMTNFILE(ATCH_FILE_ID)
);

CREATE MEMORY TABLE COMTECOPSEQ (
	TABLE_NAME   VARCHAR(16)   NOT NULL,
	NEXT_ID      DECIMAL(30),
	CONSTRAINT COMTECOPSEQ_PK PRIMARY KEY (TABLE_NAME)
);

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID) VALUES ('FILE_ID', 0);
