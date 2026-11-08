package egovframework.board;

import egovframework.board.service.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BoardIntegrationTest {
    static final Path UPLOADS;
    static { try { UPLOADS = Files.createTempDirectory("board-test-"); } catch (Exception e) { throw new RuntimeException(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("Globals.fileStorePath", UPLOADS::toString); }
    @Autowired MockMvc mvc;
    @Autowired BoardService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void clean() throws Exception {
        jdbc.update("DELETE FROM BOARD");
        try (var files = Files.list(UPLOADS)) { for (Path file : files.toList()) Files.delete(file); }
    }
    @AfterAll static void cleanup() throws Exception {
        try (var files = Files.list(UPLOADS)) { for (Path file : files.toList()) Files.delete(file); }
        Files.delete(UPLOADS);
    }
    BoardVO board(String title, String content, String writer) {
        BoardVO b = new BoardVO(); b.setTitle(title); b.setContent(content); b.setWriter(writer); return b;
    }
    MockMultipartFile file(String name) { return new MockMultipartFile("file_1", name, "text/plain", "첨부파일 테스트".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    @Test void fullCrudWithAttachments() throws Exception {
        mvc.perform(post("/addBoardView.do").with(csrf())).andExpect(status().isOk()).andExpect(view().name("board/form"));
        mvc.perform(multipart("/addBoard.do").file(file("한글 안내.txt")).file(file("추가.txt"))
            .param("title", "새로운 안내").param("content", "본문 내용").param("writer", "홍길동").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/egovBoardList.do"));
        BoardVO b = service.selectList(new BoardSearchVO()).get(0);
        assertThat(b.getBoardId()).hasSize(20);
        assertThat(service.files(b.getBoardId())).hasSize(2);
        AttachmentVO attachment = service.files(b.getBoardId()).get(0);
        mvc.perform(get("/downloadFile.do").param("fileId", attachment.getFileId()))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", startsWith("attachment;")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes("첨부파일 테스트".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(post("/updateBoardView.do").param("boardId", b.getBoardId()).with(csrf()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("한글 안내.txt")))
            .andExpect(content().string(containsString(b.getAtchFileId())));
        mvc.perform(multipart("/updateBoard.do").file(file("마지막.txt")).param("boardId", b.getBoardId())
            .param("atchFileId", b.getAtchFileId()).param("title", "수정 제목").param("content", "수정 본문").param("writer", "관리자").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(service.select(b.getBoardId()).getAtchFileId()).isEqualTo(b.getAtchFileId());
        assertThat(service.select(b.getBoardId()).getTitle()).isEqualTo("수정 제목");
        assertThat(service.files(b.getBoardId())).hasSize(3);
        mvc.perform(post("/deleteBoard.do").param("boardId", b.getBoardId()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(service.count(new BoardSearchVO())).isZero();
        try (var files = Files.list(UPLOADS)) { assertThat(files.count()).isZero(); }
        mvc.perform(get("/downloadFile.do").param("fileId", attachment.getFileId())).andExpect(status().isNotFound());
    }

    @Test void searchPaginationAndEscaping() throws Exception {
        for (int i = 0; i < 23; i++) service.insert(board("제목 " + i, "본문 " + i, "작성자 " + i), List.of());
        mvc.perform(get("/egovBoardList.do").param("pageIndex", "2"))
            .andExpect(status().isOk()).andExpect(model().attribute("boards", hasSize(10)));
        mvc.perform(get("/egovBoardList.do").param("pageIndex", "3"))
            .andExpect(status().isOk()).andExpect(model().attribute("boards", hasSize(3)));
        for (String condition : List.of("0", "1", "2")) {
            mvc.perform(get("/egovBoardList.do").param("searchCondition", condition).param("searchKeyword", "22"))
                .andExpect(status().isOk()).andExpect(model().attribute("boards", hasSize(1)));
        }
        service.insert(board("<script>alert(1)</script> 100%_", "본문", "작성자"), List.of());
        mvc.perform(get("/egovBoardList.do").param("searchKeyword", "%_"))
            .andExpect(status().isOk()).andExpect(model().attribute("boards", hasSize(1)))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>"))));
        mvc.perform(get("/egovBoardList.do").param("searchKeyword", "' OR 1=1 --"))
            .andExpect(status().isOk()).andExpect(model().attribute("boards", hasSize(0)));
    }

    @Test void rejectsCsrfInvalidFieldsAndPaths() throws Exception {
        mvc.perform(post("/deleteBoard.do").param("boardId", "anything")).andExpect(status().isForbidden());
        mvc.perform(multipart("/addBoard.do").param("title", " ").param("writer", "홍길동").with(csrf()))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("board", "title"));
        mvc.perform(multipart("/addBoard.do").param("title", "x".repeat(201)).param("content", "x".repeat(4001)).param("writer", "x".repeat(51)).with(csrf()))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("board", "title", "content", "writer"));
        for (String name : List.of("../outside.txt", "bad.jsp", "a\\b.txt", "bad\r\n.txt")) {
            mvc.perform(multipart("/addBoard.do").file(file(name)).param("title", "파일").param("writer", "작성자").with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("board"));
        }
        assertThat(service.count(new BoardSearchVO())).isZero();
        mvc.perform(get("/egovBoardList.do").param("pageIndex", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/egovBoardList.do").param("searchCondition", "3")).andExpect(status().isBadRequest());
        mvc.perform(get("/downloadFile.do").param("fileId", "../../TASK.md")).andExpect(status().isNotFound());
    }

    @Test void attachmentGroupTamperingAndCountLimitAreAtomic() throws Exception {
        BoardVO b = board("제목", "내용", "작성자"); service.insert(b, List.of(file("원본.txt")));
        mvc.perform(multipart("/updateBoard.do").file(file("실패.txt")).param("boardId", b.getBoardId())
            .param("atchFileId", UUID.randomUUID().toString()).param("title", "변조").param("writer", "작성자").with(csrf()))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("board"));
        assertThat(service.select(b.getBoardId()).getTitle()).isEqualTo("제목");
        assertThat(service.files(b.getBoardId())).hasSize(1);
        List<org.springframework.web.multipart.MultipartFile> many = new ArrayList<>();
        for (int i = 0; i < 10; i++) many.add(file("파일" + i + ".txt"));
        assertThatThrownBy(() -> service.update(b, many)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.files(b.getBoardId())).hasSize(1);
    }
}
