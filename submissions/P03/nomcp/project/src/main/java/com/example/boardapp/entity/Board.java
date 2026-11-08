package com.example.boardapp.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티.
 */
@Entity
@Table(name = "BOARD")
public class Board {

    @Id
    @Column(name = "BOARD_ID", length = 20)
    private String boardId;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "WRITER", nullable = false, length = 50)
    private String writer;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    // getters & setters
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

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        attachment.setBoard(this);
    }

    public void removeAttachment(Attachment attachment) {
        attachments.remove(attachment);
        attachment.setBoard(null);
    }
}
