package egovframework.example.board.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import egovframework.example.board.service.BoardSearchVO;
import egovframework.example.board.service.BoardVO;
import egovframework.example.board.service.EgovBoardService;
import egovframework.example.cmmn.file.service.EgovFileMngService;
import jakarta.annotation.Resource;

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
    @Transactional(readOnly = true)
    public List<BoardVO> selectBoardList(BoardSearchVO searchVO) throws Exception {
        return boardDAO.selectBoardList(searchVO);
    }

    @Override
    @Transactional(readOnly = true)
    public int selectBoardListCnt(BoardSearchVO searchVO) throws Exception {
        return boardDAO.selectBoardListCnt(searchVO);
    }

    @Override
    @Transactional(readOnly = true)
    public BoardVO selectBoard(String boardId) throws Exception {
        return boardDAO.selectBoard(boardId);
    }

    @Override
    public void insertBoard(BoardVO boardVO, List<MultipartFile> files) throws Exception {
        boardVO.setBoardId(boardIdGnrService.getNextStringId());
        boardVO.setAtchFileId(fileMngService.saveFiles(files, null));
        boardDAO.insertBoard(boardVO);
    }

    @Override
    public void updateBoard(BoardVO boardVO, List<MultipartFile> files) throws Exception {
        // 첨부 묶음은 요청값을 믿지 않고 게시물에 저장된 값을 쓴다. 다른 글의 첨부 묶음에 끼어드는 것을 막는다.
        BoardVO current = findBoard(boardVO.getBoardId());
        boardVO.setAtchFileId(fileMngService.saveFiles(files, current.getAtchFileId()));
        boardDAO.updateBoard(boardVO);
    }

    @Override
    public void deleteBoard(String boardId) throws Exception {
        BoardVO current = findBoard(boardId);
        boardDAO.deleteBoard(boardId);
        fileMngService.deleteFiles(current.getAtchFileId());
    }

    private BoardVO findBoard(String boardId) throws Exception {
        BoardVO board = boardDAO.selectBoard(boardId);
        if (board == null) {
            throw processException("fail.board.notFound");
        }
        return board;
    }
}
