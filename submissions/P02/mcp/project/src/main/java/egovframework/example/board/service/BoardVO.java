package egovframework.example.board.service;

import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

import jakarta.validation.constraints.Size;

public class BoardVO extends BoardDefaultVO {

	private static final long serialVersionUID = 1L;

	private String boardId;
	@EgovNullCheck(message="{confirm.required.board.title}")
	@Size(max = 200)
	private String title;
	@Size(max = 4000)
	private String content;
	@EgovNullCheck(message="{confirm.required.board.writer}")
	@Size(max = 50)
	private String writer;
	/** 첨부파일 그룹 키 (COMTNFILE.ATCH_FILE_ID — CHAR(20)). EgovFileMngService 가 채번/관리. */
	private String atchFileId;

	public String getBoardId() {
		return boardId;
	}

	public void setBoardId(String boardId) {
		this.boardId = boardId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getWriter() {
		return writer;
	}

	public void setWriter(String writer) {
		this.writer = writer;
	}

	public String getAtchFileId() {
		return atchFileId;
	}

	public void setAtchFileId(String atchFileId) {
		this.atchFileId = atchFileId;
	}
}
