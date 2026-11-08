package egovframework.board.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BoardVO {
    private String boardId;
    @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자 이내로 입력해 주세요.")
    private String title;
    @Size(max = 4000, message = "내용은 4,000자 이내로 입력해 주세요.")
    private String content;
    @NotBlank(message = "작성자를 입력해 주세요.") @Size(max = 50, message = "작성자는 50자 이내로 입력해 주세요.")
    private String writer;
    private String atchFileId;
    private String createdAt;
    private int fileCount;
    public String getBoardId() { return boardId; }
    public void setBoardId(String v) { boardId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getWriter() { return writer; }
    public void setWriter(String v) { writer = v; }
    public String getAtchFileId() { return atchFileId; }
    public void setAtchFileId(String v) { atchFileId = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
    public int getFileCount() { return fileCount; }
    public void setFileCount(int v) { fileCount = v; }
}
