package egovframework.board.service.impl;

import java.util.List;
import egovframework.board.service.*;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BoardDAO extends EgovAbstractMapper {
    public List<BoardVO> findAll(BoardSearchVO s) { return selectList("Board.list", s); }
    public int count(BoardSearchVO s) { return selectOne("Board.count", s); }
    public BoardVO find(String id) { return selectOne("Board.find", id); }
    public String lock(String id) { return selectOne("Board.lock", id); }
    public void insertBoard(BoardVO b) { insert("Board.insert", b); }
    public void updateBoard(BoardVO b) { update("Board.update", b); }
    public void deleteBoard(String id) { delete("Board.delete", id); }
    public List<AttachmentVO> files(String id) { return selectList("Board.files", id); }
    public AttachmentVO file(String id) { return selectOne("Board.file", id); }
    public void insertFile(AttachmentVO f) { insert("Board.insertFile", f); }
}
