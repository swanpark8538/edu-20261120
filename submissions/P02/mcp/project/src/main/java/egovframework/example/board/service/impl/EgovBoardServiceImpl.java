package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import egovframework.example.board.service.EgovBoardService;
import egovframework.example.board.service.BoardVO;
import egovframework.example.cmm.service.EgovFileMngService;
import egovframework.example.cmm.service.FileVO;
import jakarta.annotation.Resource;

@Service("boardService")
public class EgovBoardServiceImpl extends EgovAbstractServiceImpl implements EgovBoardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovBoardServiceImpl.class);

	@Resource(name = "boardMapper")
	private BoardMapper boardMapper;

	@Resource(name = "egovBoardIdGnrService")
	private EgovIdGnrService egovBoardIdGnrService;

	/** 도메인 삭제 시 첨부파일 그룹도 함께 정리 (orphan 방지) — deleteAllFileInf 호출용. */
	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileMngService;

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
		// 삭제 요청은 boardId 만 전달하므로 첨부 그룹 키는 DB 에서 조회한다.
		String atchFileId = selectBoard(vo).getAtchFileId();
		boardMapper.deleteBoard(vo);
		// 첨부 그룹 논리 삭제 (USE_AT='N') — 디스크 파일은 운영 정책에 따라 별도 cleanup.
		if (atchFileId != null && !atchFileId.isBlank()) {
			FileVO fvo = new FileVO();
			fvo.setAtchFileId(atchFileId);
			fileMngService.deleteAllFileInf(fvo);
		}
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
