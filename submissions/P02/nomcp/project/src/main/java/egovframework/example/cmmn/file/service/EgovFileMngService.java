package egovframework.example.cmmn.file.service;

import java.nio.file.Path;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일 관리 서비스.
 */
public interface EgovFileMngService {

    /**
     * 업로드 파일을 검증하여 저장한다.
     *
     * @param files      업로드 파일 목록
     * @param atchFileId 기존 첨부 묶음 ID (없으면 null)
     * @return 파일이 속한 첨부 묶음 ID. 저장할 파일이 없으면 전달받은 값을 그대로 돌려준다.
     */
    String saveFiles(List<MultipartFile> files, String atchFileId) throws Exception;

    List<FileVO> selectFileList(String atchFileId) throws Exception;

    FileVO selectFile(String atchFileId, int fileSn) throws Exception;

    /** 저장 파일의 실제 경로를 돌려준다. 파일이 없으면 업무 예외가 발생한다. */
    Path resolveStoredFile(FileVO file) throws Exception;

    /** 첨부 묶음을 삭제한다. 실제 파일은 트랜잭션 완료 후 삭제한다. */
    void deleteFiles(String atchFileId) throws Exception;
}
