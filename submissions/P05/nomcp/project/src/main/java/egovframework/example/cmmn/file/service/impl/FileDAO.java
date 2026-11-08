package egovframework.example.cmmn.file.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import egovframework.example.cmmn.file.service.FileVO;

@Repository("fileDAO")
public class FileDAO extends EgovAbstractMapper {

    public int selectNextFileSn(String atchFileId) {
        return selectOne("fileDAO.selectNextFileSn", atchFileId);
    }

    public void insertFile(FileVO file) {
        insert("fileDAO.insertFile", file);
    }

    public List<FileVO> selectFileList(String atchFileId) {
        return selectList("fileDAO.selectFileList", atchFileId);
    }

    public FileVO selectFile(FileVO key) {
        return selectOne("fileDAO.selectFile", key);
    }

    public void deleteFiles(String atchFileId) {
        delete("fileDAO.deleteFiles", atchFileId);
    }
}
