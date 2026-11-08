package egovframework.example.cmmn.file.service;

import java.nio.file.Path;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface EgovFileMngService {

    /**
     * 파일을 검증해 저장하고 첨부 묶음 ID 를 돌려준다.
     *
     * @param atchFileId 기존 묶음 ID. 없으면 올라온 파일이 있을 때만 새로 채번한다.
     */
    String saveFiles(List<MultipartFile> files, String atchFileId) throws Exception;

    List<FileVO> selectFileList(String atchFileId) throws Exception;

    FileVO selectFile(String atchFileId, int fileSn) throws Exception;

    /** 저장 폴더 안의 실제 파일 경로를 돌려준다. */
    Path resolveStoredFile(FileVO file) throws Exception;

    /** 묶음의 메타데이터를 지우고, 트랜잭션이 커밋되면 실제 파일도 지운다. */
    void deleteFiles(String atchFileId) throws Exception;
}
