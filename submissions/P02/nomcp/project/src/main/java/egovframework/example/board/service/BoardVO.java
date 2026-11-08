package egovframework.example.board.service;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시물 VO.
 */
public class BoardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String boardId;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해 주세요.")
    private String title;

    @Size(max = 4000, message = "내용은 4000자 이내로 입력해 주세요.")
    private String content;

    @NotBlank(message = "작성자를 입력해 주세요.")
    @Size(max = 50, message = "작성자는 50자 이내로 입력해 주세요.")
    private String writer;

    private String atchFileId;
    private String regDate;

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

    public String getRegDate() {
        return regDate;
    }

    public void setRegDate(String regDate) {
        this.regDate = regDate;
    }
}
