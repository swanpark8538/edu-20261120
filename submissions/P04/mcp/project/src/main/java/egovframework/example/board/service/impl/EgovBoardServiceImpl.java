package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import egovframework.example.board.service.EgovBoardService;
import egovframework.example.board.service.BoardVO;
import jakarta.annotation.Resource;

@Service("boardService")
public class EgovBoardServiceImpl extends EgovAbstractServiceImpl implements EgovBoardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovBoardServiceImpl.class);

	@Resource(name = "boardMapper")
	private BoardMapper boardMapper;

	@Resource(name = "egovBoardIdGnrService")
	private EgovIdGnrService egovBoardIdGnrService;

	@Override
	public void insertBoard(BoardVO vo) throws Exception {
		// PII 노출 방지: VO 전체(vo.toString()) 가 아니라 생성된 PK 만 로깅.
		// parameterized form 을 사용해 DEBUG 비활성 시 문자열 조립 비용도 회피.
		String id = egovBoardIdGnrService.getNextStringId();
		vo.setBoardId(id);
		LOGGER.debug("insertBoard: boardId={}", id);
		boardMapper.insertBoard(vo);
	}

	@Override
	public void updateBoard(BoardVO vo) throws Exception {
		boardMapper.updateBoard(vo);
	}

	@Override
	public void deleteBoard(BoardVO vo) throws Exception {
		boardMapper.deleteBoard(vo);
	}

	@Override
	public BoardVO selectBoard(BoardVO vo) throws Exception {
		BoardVO resultVO = boardMapper.selectBoard(vo);
		if (resultVO == null)
			throw processException("info.nodata.msg");
		return resultVO;
	}

	@Override
	public List<?> selectBoardList(BoardVO vo) throws Exception {
		return boardMapper.selectBoardList(vo);
	}

	@Override
	public int selectBoardListTotCnt(BoardVO vo) {
		return boardMapper.selectBoardListTotCnt(vo);
	}

}
