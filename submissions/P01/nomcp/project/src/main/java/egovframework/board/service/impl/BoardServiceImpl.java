package egovframework.board.service.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import egovframework.board.service.*;

@Service
@Transactional(readOnly=true)
public class BoardServiceImpl extends EgovAbstractServiceImpl implements BoardService {
 private final BoardDAO dao; private final FileStorage storage; private final EgovTableIdGnrServiceImpl ids;
 public BoardServiceImpl(BoardDAO dao,FileStorage storage,EgovTableIdGnrServiceImpl ids){this.dao=dao;this.storage=storage;this.ids=ids;}
 public List<Board> list(BoardSearch s){return dao.list(s);} public int count(BoardSearch s){return dao.count(s);}
 public Board get(String id){Board b=dao.get(id); if(b==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"게시물을 찾을 수 없습니다.");return b;}
 public List<Attachment> files(String id){get(id);return dao.files(id);}
 public Attachment file(String id){var f=dao.file(id);if(f==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"첨부파일을 찾을 수 없습니다.");return f;}
 @Transactional(rollbackFor=Exception.class)
 public String create(Board b,List<MultipartFile> files) throws Exception {
  storage.validate(files);
  b.setBoardId("BOARD_"+String.format("%014d",ids.getNextLongId()));
  b.setAtchFileId(UUID.randomUUID().toString()); dao.add(b); attach(b.getBoardId(),files);return b.getBoardId();
 }
 @Transactional(rollbackFor=Exception.class)
 public void update(Board b,List<MultipartFile> files) throws Exception {
  Board old=get(b.getBoardId());
  if(b.getAtchFileId()!=null && !b.getAtchFileId().isEmpty() && !old.getAtchFileId().equals(b.getAtchFileId()))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"첨부파일 묶음이 일치하지 않습니다.");
  b.setAtchFileId(old.getAtchFileId()); storage.validate(files);
  if(dao.edit(b)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"게시물을 찾을 수 없습니다.");
  attach(b.getBoardId(),files);
 }
 private void attach(String id,List<MultipartFile> files) throws Exception {
  for(var file:files) {
   if(file.isEmpty())continue;
   Attachment saved=storage.save(id,file);
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
    @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.remove(saved);}
   });
   dao.addFile(saved);
  }
 }
 @Transactional
 public void delete(String id){
  get(id); var files=dao.files(id);dao.remove(id);
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
   @Override public void afterCommit(){files.forEach(storage::remove);}
  });
 }
}
