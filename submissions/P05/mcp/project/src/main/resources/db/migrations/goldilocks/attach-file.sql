-- ============================================================
-- 첨부파일 인프라 DDL — goldilocks
-- ============================================================
-- 두 테이블 (COMTNFILE 마스터, COMTNFILEDETAIL 상세) + 시퀀스 관리 테이블 (COMTECOPSEQ)
-- + COMTECOPSEQ 의 FILE_ID 시퀀스 row 시드.
--
-- 컬럼 / FK 정의는 공통컴포넌트 표준과 호환된다 — 공통 다운로드/관리 화면이 그대로
-- 인식할 수 있다.
--
-- 호출자: egov_add_attach_file_infrastructure 도구 (mode=append-fallback).
-- 시드 SQL 이 정확히 1개일 때만 기존 시드에 append, 아니면 본 파일이 별도로 적용된다.

CREATE TABLE COMTNFILE (
    ATCH_FILE_ID  CHAR(20)        NOT NULL,
    CREAT_DT      DATE            NOT NULL,
    USE_AT        CHAR(1),
    CONSTRAINT COMTNFILE_PK PRIMARY KEY (ATCH_FILE_ID)
);

CREATE TABLE COMTNFILEDETAIL (
    ATCH_FILE_ID     CHAR(20)        NOT NULL,
    FILE_SN          NUMBER(10)      NOT NULL,
    FILE_STRE_COURS  VARCHAR(2000)   NOT NULL,
    STRE_FILE_NM     VARCHAR(255)    NOT NULL,
    ORIGNL_FILE_NM   VARCHAR(255),
    FILE_EXTSN       VARCHAR(20)     NOT NULL,
    FILE_CN          CLOB,
    FILE_SIZE        NUMBER(8),
    CONSTRAINT COMTNFILEDETAIL_PK PRIMARY KEY (ATCH_FILE_ID, FILE_SN),
    CONSTRAINT COMTNFILEDETAIL_FK1 FOREIGN KEY (ATCH_FILE_ID) REFERENCES COMTNFILE(ATCH_FILE_ID)
);

CREATE TABLE COMTECOPSEQ (
    TABLE_NAME   VARCHAR(16)  NOT NULL,
    NEXT_ID      NUMBER(30),
    CONSTRAINT COMTECOPSEQ_PK PRIMARY KEY (TABLE_NAME)
);

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID) VALUES ('FILE_ID', 0);
