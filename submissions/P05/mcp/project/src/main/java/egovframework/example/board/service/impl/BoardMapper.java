package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import egovframework.example.board.service.BoardVO;

@EgovMapper("boardMapper")
public interface BoardMapper {

	void insertBoard(BoardVO vo) throws Exception;

	void updateBoard(BoardVO vo) throws Exception;

	void deleteBoard(BoardVO vo) throws Exception;

	BoardVO selectBoard(BoardVO vo) throws Exception;

	List<?> selectBoardList(BoardVO vo) throws Exception;

	int selectBoardListTotCnt(BoardVO vo);

}
