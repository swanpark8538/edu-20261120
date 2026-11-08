package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import egovframework.example.board.service.BoardDefaultVO;
import egovframework.example.board.service.BoardVO;
import egovframework.example.board.service.EgovBoardService;
import egovframework.example.cmmn.file.service.EgovFileMngService;
import jakarta.annotation.Resource;

/**
 * 게시물 관리 서비스 구현.
 */
@Service("boardService")
@Transactional(rollbackFor = Exception.class)
public class EgovBoardServiceImpl extends EgovAbstractServiceImpl implements EgovBoardService {

    @Resource(name = "boardDAO")
    private BoardDAO boardDAO;

    @Resource(name = "boardIdGnrService")
    private EgovIdGnrService boardIdGnrService;

    @Resource(name = "fileMngService")
    private EgovFileMngService fileMngService;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<BoardVO> selectBoardList(BoardDefaultVO searchVO) {
        return boardDAO.selectBoardList(searchVO);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public int selectBoardListTotCnt(BoardDefaultVO searchVO) {
        return boardDAO.selectBoardListTotCnt(searchVO);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public BoardVO selectBoard(String boardId) throws Exception {
        BoardVO board = boardDAO.selectBoard(boardId);
        if (board == null) {
            throw processException("fail.board.notFound");
        }
        return board;
    }

    @Override
    public void insertBoard(BoardVO board, List<MultipartFile> files) throws Exception {
        board.setBoardId(boardIdGnrService.getNextStringId());
        board.setAtchFileId(fileMngService.saveFiles(files, null));
        boardDAO.insertBoard(board);
    }

    @Override
    public void updateBoard(BoardVO board, List<MultipartFile> files) throws Exception {
        BoardVO stored = selectBoard(board.getBoardId());
        board.setAtchFileId(fileMngService.saveFiles(files, stored.getAtchFileId()));
        boardDAO.updateBoard(board);
    }

    @Override
    public void deleteBoard(String boardId) throws Exception {
        BoardVO stored = selectBoard(boardId);
        boardDAO.deleteBoard(boardId);
        fileMngService.deleteFiles(stored.getAtchFileId());
    }
}
