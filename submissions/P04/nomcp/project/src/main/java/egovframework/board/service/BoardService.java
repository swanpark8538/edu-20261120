package egovframework.board.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface BoardService {
    List<BoardVO> selectList(BoardSearchVO search);
    int count(BoardSearchVO search);
    BoardVO select(String boardId);
    List<AttachmentVO> files(String boardId);
    String insert(BoardVO board, List<MultipartFile> files) throws Exception;
    void update(BoardVO board, List<MultipartFile> files) throws Exception;
    void delete(String boardId);
    AttachmentVO file(String fileId);
}
