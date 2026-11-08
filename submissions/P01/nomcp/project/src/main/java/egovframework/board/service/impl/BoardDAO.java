package egovframework.board.service.impl;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import egovframework.board.service.*;
@Repository
public class BoardDAO extends EgovAbstractMapper {
 public List<Board> list(BoardSearch s){return selectList("Board.list",s);}
 public int count(BoardSearch s){return selectOne("Board.count",s);}
 public Board get(String id){return selectOne("Board.get",id);}
 public void add(Board b){insert("Board.add",b);}
 public int edit(Board b){return update("Board.edit",b);}
 public int remove(String id){return delete("Board.remove",id);}
 public List<Attachment> files(String id){return selectList("Board.files",id);}
 public Attachment file(String id){return selectOne("Board.file",id);}
 public void addFile(Attachment f){insert("Board.addFile",f);}
}
