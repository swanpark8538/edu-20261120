package egovframework.board;

import egovframework.board.service.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BoardIntegrationTest {
 static Path uploads;
 static { try{uploads=Files.createTempDirectory("board-test-");}catch(Exception ex){throw new RuntimeException(ex);} }
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("Globals.fileStorePath",()->uploads.toString());}
 @Autowired MockMvc mvc;
 @Autowired BoardService service;
 @AfterEach void clear(){while(service.count(new BoardSearch())>0)for(Board b:service.list(new BoardSearch())) service.delete(b.getBoardId());}
 @AfterAll static void cleanFiles() throws Exception {try(var paths=Files.list(uploads)){for(var p:paths.toList())Files.deleteIfExists(p);}Files.deleteIfExists(uploads);}
 MockMultipartFile file(String name,String text){return new MockMultipartFile("file_1",name,"text/plain",text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 String create(String title,String content,String writer) throws Exception {
  mvc.perform(multipart("/addBoard.do").param("title",title).param("content",content).param("writer",writer).with(csrf())).andExpect(status().is3xxRedirection());
  return service.list(new BoardSearch()).get(0).getBoardId();
 }
 @Test void completeCrudWithMultipleFilesAndPreservedGroup() throws Exception {
  mvc.perform(get("/egovBoardList.do")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("아직 등록된")));
  mvc.perform(post("/addBoardView.do").with(csrf())).andExpect(status().isOk()).andExpect(view().name("form"));
  mvc.perform(multipart("/addBoard.do").file(file("안내.txt","first")).file(file("두번째.txt","second")).param("title","안내").param("content","본문").param("writer","홍길동").with(csrf())).andExpect(status().is3xxRedirection());
  Board board=service.list(new BoardSearch()).get(0);String id=board.getBoardId();String group=board.getAtchFileId();
  assertThat(id).hasSize(20);assertThat(service.files(id)).hasSize(2);
  var original=service.files(id).get(0);
  mvc.perform(get("/boardDetail.do").param("boardId",id)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("안내")));
  mvc.perform(post("/updateBoardView.do").param("boardId",id).with(csrf())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("atchFileId")));
  mvc.perform(get("/downloadFile.do").param("fileId",original.fileId())).andExpect(status().isOk()).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")));
  mvc.perform(multipart("/updateBoard.do").file(file("추가.txt","third")).param("boardId",id).param("atchFileId",group).param("title","변경").param("content","변경 본문").param("writer","김담당").with(csrf())).andExpect(status().is3xxRedirection());
  assertThat(service.get(id).getAtchFileId()).isEqualTo(group);assertThat(service.get(id).getTitle()).isEqualTo("변경");assertThat(service.files(id)).hasSize(3);
  mvc.perform(post("/deleteBoard.do").param("boardId",id).with(csrf())).andExpect(status().is3xxRedirection());
  assertThat(service.count(new BoardSearch())).isZero();try(var paths=Files.list(uploads)){assertThat(paths.toList()).isEmpty();}
  mvc.perform(get("/downloadFile.do").param("fileId",original.fileId())).andExpect(status().isNotFound());
 }
 @Test void searchAllConditionsPaginationAndLiteralWildcards() throws Exception {
  for(int i=0;i<12;i++)create("게시물 "+i,"내용 "+i,"작성자 "+i);
  create("고유제목%_","독특한본문","특별작성자");
  mvc.perform(get("/egovBoardList.do").param("pageIndex","1")).andExpect(status().isOk()).andExpect(model().attribute("boards",org.hamcrest.Matchers.hasSize(10)));
  mvc.perform(get("/egovBoardList.do").param("pageIndex","2")).andExpect(status().isOk()).andExpect(model().attribute("boards",org.hamcrest.Matchers.hasSize(3)));
  mvc.perform(get("/egovBoardList.do").param("pageIndex","1000000")).andExpect(status().isOk()).andExpect(model().attribute("boards",org.hamcrest.Matchers.hasSize(3)));
  for(var entry:Map.of("0","고유제목","1","독특한본문","2","특별작성자").entrySet())
   mvc.perform(get("/egovBoardList.do").param("searchCondition",entry.getKey()).param("searchKeyword",entry.getValue())).andExpect(status().isOk()).andExpect(model().attribute("boards",org.hamcrest.Matchers.hasSize(1)));
  mvc.perform(get("/egovBoardList.do").param("searchKeyword","%_")).andExpect(status().isOk()).andExpect(model().attribute("boards",org.hamcrest.Matchers.hasSize(1)));
  mvc.perform(get("/egovBoardList.do").param("searchKeyword","' OR 1=1 --")).andExpect(status().isOk()).andExpect(model().attribute("boards",org.hamcrest.Matchers.empty()));
 }
 @Test void csrfValidationXssAndSecurityHeaders() throws Exception {
  mvc.perform(post("/deleteBoard.do").param("boardId","fake")).andExpect(status().isForbidden());
  mvc.perform(multipart("/addBoard.do").param("title","bad").param("writer","x")).andExpect(status().isForbidden());
  mvc.perform(multipart("/addBoard.do").param("title","").param("writer","").with(csrf())).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("board","title","writer"));
  mvc.perform(multipart("/addBoard.do").param("title","x".repeat(201)).param("writer","writer").with(csrf())).andExpect(model().attributeHasFieldErrors("board","title"));
  assertThat(service.count(new BoardSearch())).isZero();
  String id=create("<script>alert(1)</script>","<img src=x onerror=alert(1)>","<b>writer</b>");
  mvc.perform(get("/boardDetail.do").param("boardId",id)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert")))).andExpect(header().exists("Content-Security-Policy")).andExpect(header().string("X-Content-Type-Options","nosniff"));
  mvc.perform(get("/egovBoardList.do").param("pageIndex","0")).andExpect(status().isBadRequest());
  mvc.perform(get("/egovBoardList.do").param("searchCondition","TITLE;DROP")).andExpect(status().isBadRequest());
  mvc.perform(get("/boardDetail.do").param("boardId","none")).andExpect(status().isNotFound());
 }
 @Test void uploadFailureRollsBackMetadataAndPhysicalFiles() throws Exception {
  var board=new Board();board.setTitle("롤백 검증");board.setWriter("담당자");board.setContent("내용");
  var broken=new MockMultipartFile("file_1","실패.txt","text/plain",new byte[]{1}) {
   @Override public java.io.InputStream getInputStream() throws java.io.IOException {throw new java.io.IOException("simulated disk/read failure");}
  };
  assertThatThrownBy(()->service.create(board,List.of(file("정상.txt","first"),broken))).isInstanceOf(java.io.IOException.class);
  assertThat(service.count(new BoardSearch())).isZero();
  try(var paths=Files.list(uploads)){assertThat(paths.toList()).isEmpty();}
 }
 @Test void unsafeFilesAndGroupTamperingAreRejectedWithoutMutation() throws Exception {
  for(String name:List.of("../../evil.txt","evil.jsp","evil.html","bad\\name.txt")) {
   mvc.perform(multipart("/addBoard.do").file(file(name,"bad")).param("title","x").param("writer","x").with(csrf())).andExpect(status().isOk()).andExpect(model().attributeExists("formError"));
  }
  assertThat(service.count(new BoardSearch())).isZero();
  String id=create("정상","내용","작성자");
  mvc.perform(multipart("/updateBoard.do").param("boardId",id).param("atchFileId",UUID.randomUUID().toString()).param("title","변조").param("writer","변조").with(csrf())).andExpect(model().attributeExists("formError"));
  assertThat(service.get(id).getTitle()).isEqualTo("정상");
  mvc.perform(get("/downloadFile.do").param("fileId","../../etc/passwd")).andExpect(status().isNotFound());
 }
}
