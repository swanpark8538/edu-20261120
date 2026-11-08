package egovframework.board.service;
public record Attachment(String fileId, String boardId, String originalName, String storedName, long fileSize) {}
