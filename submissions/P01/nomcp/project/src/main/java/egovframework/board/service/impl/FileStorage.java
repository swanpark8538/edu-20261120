package egovframework.board.service.impl;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import egovframework.board.service.Attachment;

@Component
public class FileStorage {
 private static final Logger log=LoggerFactory.getLogger(FileStorage.class);
 private static final Set<String> ALLOWED=Set.of("txt","csv","pdf","png","jpg","jpeg","gif","webp","zip","hwp","hwpx","doc","docx","xls","xlsx","ppt","pptx","odt");
 private final Path root;
 public FileStorage(@Value("${Globals.fileStorePath}") String path) throws IOException {
  root=Path.of(path).toAbsolutePath().normalize(); Files.createDirectories(root);
 }
 public void validate(List<MultipartFile> files) {
  long total=0; int count=0;
  for(var file:files) {
   if(file.isEmpty()) continue;
   count++; total+=file.getSize(); String name=file.getOriginalFilename();
   if(name==null || name.isBlank() || name.length()>200 || name.contains("/") || name.contains("\\") || name.codePoints().anyMatch(Character::isISOControl))
    throw bad("파일 이름을 확인해 주세요. 경로나 제어문자는 허용하지 않습니다.");
   int dot=name.lastIndexOf('.');
   if(dot<1 || !ALLOWED.contains(name.substring(dot+1).toLowerCase(Locale.ROOT))) throw bad("허용되지 않는 파일 형식입니다.");
   if(file.getSize()>10*1024*1024) throw bad("첨부파일은 개당 10MB 이내로 등록해 주세요.");
  }
  if(count>10 || total>45L*1024*1024) throw bad("한 번에 10개, 합계 45MB까지 첨부할 수 있습니다.");
 }
 public Attachment save(String boardId,MultipartFile file) throws IOException {
  String id=UUID.randomUUID().toString(); Path path=resolve(id);
  try(InputStream in=file.getInputStream()) {
   Files.copy(in,path);
  } catch(IOException ex) { Files.deleteIfExists(path); throw ex; }
  return new Attachment(id,boardId,file.getOriginalFilename(),id,file.getSize());
 }
 public Path resolve(String name) {
  if(name==null || !name.matches("[a-f0-9-]{36}")) throw bad("파일 식별자가 올바르지 않습니다.");
  Path path=root.resolve(name).normalize();
  if(!path.getParent().equals(root) || Files.isSymbolicLink(path)) throw bad("허용되지 않는 파일 경로입니다.");
  return path;
 }
 public void remove(Attachment file) {
  try { Files.deleteIfExists(resolve(file.storedName())); }
  catch(IOException ex) { log.error("첨부파일 정리에 실패했습니다: {}",file.fileId(),ex); }
 }
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
}
