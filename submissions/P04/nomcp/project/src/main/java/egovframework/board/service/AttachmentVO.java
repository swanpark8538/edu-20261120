package egovframework.board.service;

public class AttachmentVO {
    private String fileId;
    private String boardId;
    private String originalName;
    private String storedName;
    private long fileSize;
    public String getFileId() { return fileId; }
    public void setFileId(String v) { fileId = v; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String v) { boardId = v; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String v) { originalName = v; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String v) { storedName = v; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long v) { fileSize = v; }
}
