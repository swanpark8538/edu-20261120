package egovframework.board.service;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
public interface BoardService {
 List<Board> list(BoardSearch search); int count(BoardSearch search); Board get(String id);
 List<Attachment> files(String id); Attachment file(String id);
 String create(Board board, List<MultipartFile> files) throws Exception;
 void update(Board board, List<MultipartFile> files) throws Exception;
 void delete(String id);
}
