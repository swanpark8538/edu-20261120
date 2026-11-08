package egovframework.example.board.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface EgovBoardService {

	void insertBoard(BoardVO vo, List<MultipartFile> files) throws Exception;

	void updateBoard(BoardVO vo, List<MultipartFile> files) throws Exception;

	void deleteBoard(BoardVO vo) throws Exception;

	BoardVO selectBoard(BoardVO vo) throws Exception;

	List<?> selectBoardList(BoardVO vo) throws Exception;

	int selectBoardListTotCnt(BoardVO vo);

}
