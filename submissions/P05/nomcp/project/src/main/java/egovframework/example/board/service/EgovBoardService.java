package egovframework.example.board.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface EgovBoardService {

    List<BoardVO> selectBoardList(BoardSearchVO searchVO) throws Exception;

    int selectBoardListCnt(BoardSearchVO searchVO) throws Exception;

    BoardVO selectBoard(String boardId) throws Exception;

    void insertBoard(BoardVO boardVO, List<MultipartFile> files) throws Exception;

    void updateBoard(BoardVO boardVO, List<MultipartFile> files) throws Exception;

    void deleteBoard(String boardId) throws Exception;
}
