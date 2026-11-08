package egovframework.example.board.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * 게시물 관리 서비스.
 */
public interface EgovBoardService {

    List<BoardVO> selectBoardList(BoardDefaultVO searchVO) throws Exception;

    int selectBoardListTotCnt(BoardDefaultVO searchVO) throws Exception;

    /** 게시물을 조회한다. 없으면 업무 예외가 발생한다. */
    BoardVO selectBoard(String boardId) throws Exception;

    void insertBoard(BoardVO board, List<MultipartFile> files) throws Exception;

    /** 게시물을 수정한다. 첨부 묶음은 저장된 값을 유지하고 요청의 atchFileId 로 바꾸지 않는다. */
    void updateBoard(BoardVO board, List<MultipartFile> files) throws Exception;

    void deleteBoard(String boardId) throws Exception;
}
