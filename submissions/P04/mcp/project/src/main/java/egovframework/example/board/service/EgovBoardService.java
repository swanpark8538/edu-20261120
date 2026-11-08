package egovframework.example.board.service;

import java.util.List;

public interface EgovBoardService {

	void insertBoard(BoardVO vo) throws Exception;

	void updateBoard(BoardVO vo) throws Exception;

	void deleteBoard(BoardVO vo) throws Exception;

	BoardVO selectBoard(BoardVO vo) throws Exception;

	List<?> selectBoardList(BoardVO vo) throws Exception;

	int selectBoardListTotCnt(BoardVO vo);

}
