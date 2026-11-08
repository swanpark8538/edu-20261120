package egovframework.example.board.service;

import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

public class BoardVO extends BoardDefaultVO {

	private static final long serialVersionUID = 1L;

	private String boardId;
	@EgovNullCheck(message="{confirm.required.board.title}")
	private String title;
	@EgovNullCheck(message="{confirm.required.board.content}")
	private String content;
	@EgovNullCheck(message="{confirm.required.board.writer}")
	private String writer;
	@EgovNullCheck(message="{confirm.required.board.atchFileId}")
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
