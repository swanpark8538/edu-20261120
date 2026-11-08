package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import egovframework.example.board.service.BoardDefaultVO;
import egovframework.example.board.service.BoardVO;

/**
 * 게시물 DAO.
 */
@Repository("boardDAO")
public class BoardDAO extends EgovAbstractMapper {

    public List<BoardVO> selectBoardList(BoardDefaultVO searchVO) {
        return selectList("boardDAO.selectBoardList", searchVO);
    }

    public int selectBoardListTotCnt(BoardDefaultVO searchVO) {
        return selectOne("boardDAO.selectBoardListTotCnt", searchVO);
    }

    public BoardVO selectBoard(String boardId) {
        return selectOne("boardDAO.selectBoard", boardId);
    }

    public void insertBoard(BoardVO board) {
        insert("boardDAO.insertBoard", board);
    }

    public void updateBoard(BoardVO board) {
        update("boardDAO.updateBoard", board);
    }

    public void deleteBoard(String boardId) {
        delete("boardDAO.deleteBoard", boardId);
    }
}
