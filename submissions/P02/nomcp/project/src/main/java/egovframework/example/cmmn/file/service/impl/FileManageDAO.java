package egovframework.example.cmmn.file.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import egovframework.example.cmmn.file.service.FileVO;

/**
 * 첨부파일 DAO.
 */
@Repository("fileManageDAO")
public class FileManageDAO extends EgovAbstractMapper {

    public void insertFile(FileVO file) {
        insert("fileManageDAO.insertFile", file);
    }

    public List<FileVO> selectFileList(String atchFileId) {
        return selectList("fileManageDAO.selectFileList", atchFileId);
    }

    public FileVO selectFile(FileVO file) {
        return selectOne("fileManageDAO.selectFile", file);
    }

    public int selectMaxFileSn(String atchFileId) {
        return selectOne("fileManageDAO.selectMaxFileSn", atchFileId);
    }

    public void deleteFiles(String atchFileId) {
        delete("fileManageDAO.deleteFiles", atchFileId);
    }
}
