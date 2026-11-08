package egovframework.example.board.service;

import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

public class BoardVO extends BoardDefaultVO {

	private static final long serialVersionUID = 1L;

	private String boardId;
 private String atchFileId;
 public String getAtchFileId() { return atchFileId; }
 public void setAtchFileId(String value) { atchFileId = value; }
	@EgovNullCheck(message="{confirm.required.board.title}")
	@jakarta.validation.constraints.NotBlank
 @jakarta.validation.constraints.Size(max=200)
 private String title;
	@jakarta.validation.constraints.Size(max=4000)
	private String content;
	@EgovNullCheck(message="{confirm.required.board.writer}")
	@jakarta.validation.constraints.NotBlank
 @jakarta.validation.constraints.Size(max=50)
 private String writer;

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

}
