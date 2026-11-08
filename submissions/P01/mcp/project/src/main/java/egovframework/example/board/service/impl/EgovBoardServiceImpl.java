package egovframework.example.board.service.impl;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import egovframework.example.cmm.service.EgovFileMngService;
import egovframework.example.cmm.service.EgovFileMngUtil;
import egovframework.example.cmm.service.FileVO;

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

	@Resource(name = "EgovFileMngService")
    private EgovFileMngService fileService;
    @Resource(name = "EgovFileMngUtil")
    private EgovFileMngUtil fileUtil;

    private FileVO fileKey(String id) {
        FileVO key = new FileVO();
        key.setAtchFileId(id);
        return key;
    }

    private String saveAttachments(List<MultipartFile> files, String group) throws Exception {
        if (files == null || files.stream().noneMatch(f -> f.getOriginalFilename() != null && !f.getOriginalFilename().isEmpty())) return group;
        int next = group == null || group.isEmpty() ? 0 : fileService.getMaxFileSN(fileKey(group));
        List<FileVO> uploaded = fileUtil.parseFileInf(files, "BOARD_", next, group, "");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) uploaded.forEach(fileUtil::deleteStoredFile);
            }
        });
        if (group == null || group.isEmpty()) return fileService.insertFileInfs(uploaded);
        fileService.updateFileInfs(uploaded);
        return group;
    }

	@Override
	public void insertBoard(BoardVO vo, List<MultipartFile> files) throws Exception {
		// PII 노출 방지: VO 전체(vo.toString()) 가 아니라 생성된 PK 만 로깅.
		// parameterized form 을 사용해 DEBUG 비활성 시 문자열 조립 비용도 회피.
		String id = egovBoardIdGnrService.getNextStringId();
		vo.setBoardId(id);
		LOGGER.debug("insertBoard: boardId={}", id);
		vo.setAtchFileId(saveAttachments(files, null));
		boardMapper.insertBoard(vo);
	}

	@Override
	public void updateBoard(BoardVO vo, List<MultipartFile> files) throws Exception {
		// 그룹 식별자는 요청값 대신 DB의 기존 게시물에서 가져온다.
		BoardVO current = boardMapper.selectBoardForUpdate(vo);
        if (current == null) throw processException("info.nodata.msg");
		vo.setAtchFileId(saveAttachments(files, current.getAtchFileId()));
		boardMapper.updateBoard(vo);
	}

	@Override
	public void deleteBoard(BoardVO vo) throws Exception {
		BoardVO current = boardMapper.selectBoardForUpdate(vo);
        if (current == null) throw processException("info.nodata.msg");
        boardMapper.deleteBoard(vo);
        if (current.getAtchFileId() != null && !current.getAtchFileId().isEmpty()) {
            FileVO key = fileKey(current.getAtchFileId());
            List<FileVO> stored = fileService.selectFileInfs(key);
            fileService.deleteAllFileInf(key);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { stored.forEach(fileUtil::deleteStoredFile); }
            });
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
