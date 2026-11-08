package egovframework.example.cmm.service;

import java.io.Serializable;

/**
 * 첨부파일 정보 처리를 위한 VO 클래스.
 *
 * 모든 필드가 String — MyBatis resultMap 매핑 호환을 위해 의도적으로 유지한다.
 * Long fileSize / int fileSn 으로 바꾸면 기존 EgovFileMng_SQL.xml 의 resultMap 과 어긋난다.
 */
public class FileVO implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 첨부파일 아이디 (COMTNFILE.ATCH_FILE_ID — CHAR(20), "FILE_" + 15자리). */
	private String atchFileId = "";

	/** 생성일자. */
	private String creatDt = "";

	/** 파일내용 (CLOB/TEXT — 보통 빈값). */
	private String fileCn = "";

	/** 파일확장자. */
	private String fileExtsn = "";

	/** 파일크기 (바이트, 문자열). */
	private String fileMg = "";

	/** 파일연번 (COMTNFILEDETAIL.FILE_SN — 그룹 내 1-base). */
	private String fileSn = "";

	/** 파일저장경로. */
	private String fileStreCours = "";

	/** 원본 파일명 (사용자가 본 이름). */
	private String orignlFileNm = "";

	/** 저장 파일명 (난수+timestamp, 다운로드 시 실제 디스크 파일명). */
	private String streFileNm = "";

	public String getAtchFileId() { return atchFileId; }
	public void setAtchFileId(String atchFileId) { this.atchFileId = atchFileId; }

	public String getCreatDt() { return creatDt; }
	public void setCreatDt(String creatDt) { this.creatDt = creatDt; }

	public String getFileCn() { return fileCn; }
	public void setFileCn(String fileCn) { this.fileCn = fileCn; }

	public String getFileExtsn() { return fileExtsn; }
	public void setFileExtsn(String fileExtsn) { this.fileExtsn = fileExtsn; }

	public String getFileMg() { return fileMg; }
	public void setFileMg(String fileMg) { this.fileMg = fileMg; }

	public String getFileSn() { return fileSn; }
	public void setFileSn(String fileSn) { this.fileSn = fileSn; }

	public String getFileStreCours() { return fileStreCours; }
	public void setFileStreCours(String fileStreCours) { this.fileStreCours = fileStreCours; }

	public String getOrignlFileNm() { return orignlFileNm; }
	public void setOrignlFileNm(String orignlFileNm) { this.orignlFileNm = orignlFileNm; }

	public String getStreFileNm() { return streFileNm; }
	public void setStreFileNm(String streFileNm) { this.streFileNm = streFileNm; }
}
