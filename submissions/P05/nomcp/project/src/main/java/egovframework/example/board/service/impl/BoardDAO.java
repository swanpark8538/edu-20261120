package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import egovframework.example.board.service.BoardSearchVO;
import egovframework.example.board.service.BoardVO;

@Repository("boardDAO")
public class BoardDAO extends EgovAbstractMapper {

    public List<BoardVO> selectBoardList(BoardSearchVO searchVO) {
        return selectList("boardDAO.selectBoardList", searchVO);
    }

    public int selectBoardListCnt(BoardSearchVO searchVO) {
        return selectOne("boardDAO.selectBoardListCnt", searchVO);
    }

    public BoardVO selectBoard(String boardId) {
        return selectOne("boardDAO.selectBoard", boardId);
    }

    public void insertBoard(BoardVO boardVO) {
        insert("boardDAO.insertBoard", boardVO);
    }

    public void updateBoard(BoardVO boardVO) {
        update("boardDAO.updateBoard", boardVO);
    }

    public void deleteBoard(String boardId) {
        delete("boardDAO.deleteBoard", boardId);
    }
}
