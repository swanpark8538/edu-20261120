package egovframework.example.cmmn.file.service;

public class FileVO {

    private String atchFileId;
    private int fileSn;
    private String orignlFileNm;
    private String streFileNm;
    private long fileSize;

    public String getAtchFileId() {
        return atchFileId;
    }

    public void setAtchFileId(String atchFileId) {
        this.atchFileId = atchFileId;
    }

    public int getFileSn() {
        return fileSn;
    }

    public void setFileSn(int fileSn) {
        this.fileSn = fileSn;
    }

    public String getOrignlFileNm() {
        return orignlFileNm;
    }

    public void setOrignlFileNm(String orignlFileNm) {
        this.orignlFileNm = orignlFileNm;
    }

    public String getStreFileNm() {
        return streFileNm;
    }

    public void setStreFileNm(String streFileNm) {
        this.streFileNm = streFileNm;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /** 화면 표시용 크기(KB, 올림). */
    public long getFileSizeKb() {
        return (fileSize + 1023) / 1024;
    }
}
