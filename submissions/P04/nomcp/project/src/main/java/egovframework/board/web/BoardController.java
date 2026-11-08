package egovframework.board.web;

import egovframework.board.service.*;
import egovframework.board.service.impl.FileStorage;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BoardController {
    private final BoardService service;
    private final FileStorage storage;
    public BoardController(BoardService service, FileStorage storage) { this.service = service; this.storage = storage; }
    @InitBinder("board")
    void boardBinder(WebDataBinder binder) { binder.setAllowedFields("boardId", "title", "content", "writer", "atchFileId"); }
    @InitBinder("search")
    void searchBinder(WebDataBinder binder) { binder.setAllowedFields("pageIndex", "searchCondition", "searchKeyword"); }
    @GetMapping("/") public String home() { return "redirect:/egovBoardList.do"; }

    @GetMapping("/egovBoardList.do")
    public String list(@Valid @ModelAttribute("search") BoardSearchVO search, Model model) {
        int total = service.count(search);
        int last = Math.max(1, (total + 9) / 10);
        search.setPageIndex(Math.min(search.getPageIndex(), last));
        PaginationInfo page = new PaginationInfo();
        page.setCurrentPageNo(search.getPageIndex()); page.setRecordCountPerPage(10);
        page.setPageSize(10); page.setTotalRecordCount(total);
        model.addAttribute("pagination", page);
        model.addAttribute("boards", service.selectList(search));
        return "board/list";
    }
    @PostMapping("/addBoardView.do")
    public String addView(Model model) { return form(new BoardVO(), model, false); }
    @PostMapping("/updateBoardView.do")
    public String updateView(@RequestParam String boardId, Model model) { return form(service.select(boardId), model, true); }

    @PostMapping("/addBoard.do")
    public String add(@Valid @ModelAttribute("board") BoardVO board, BindingResult errors,
                      @RequestParam(value = "file_1", required = false) List<MultipartFile> files,
                      Model model, RedirectAttributes redirect) throws Exception {
        if (errors.hasErrors()) return form(board, model, false);
        try { service.insert(board, files); }
        catch (IllegalArgumentException e) { errors.reject("upload", e.getMessage()); return form(board, model, false); }
        redirect.addFlashAttribute("message", "게시물을 등록했습니다.");
        return "redirect:/egovBoardList.do";
    }
    @PostMapping("/updateBoard.do")
    public String update(@Valid @ModelAttribute("board") BoardVO board, BindingResult errors,
                         @RequestParam(value = "file_1", required = false) List<MultipartFile> files,
                         Model model, RedirectAttributes redirect) throws Exception {
        service.select(board.getBoardId());
        if (errors.hasErrors()) return form(board, model, true);
        try { service.update(board, files); }
        catch (IllegalArgumentException e) { errors.reject("upload", e.getMessage()); return form(board, model, true); }
        redirect.addFlashAttribute("message", "게시물을 수정했습니다.");
        return "redirect:/egovBoardList.do";
    }
    @PostMapping("/deleteBoard.do")
    public String delete(@RequestParam String boardId, RedirectAttributes redirect) {
        service.delete(boardId); redirect.addFlashAttribute("message", "게시물을 삭제했습니다.");
        return "redirect:/egovBoardList.do";
    }
    @GetMapping("/downloadFile.do")
    public ResponseEntity<Resource> download(@RequestParam String fileId) throws Exception {
        AttachmentVO file = service.file(fileId);
        Path path = storage.resolve(file.getStoredName());
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(Files.size(path)).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.getOriginalName(), StandardCharsets.UTF_8).build().toString())
            .body(new FileSystemResource(path));
    }
    private String form(BoardVO board, Model model, boolean editing) {
        if (!model.containsAttribute("board")) model.addAttribute("board", board);
        model.addAttribute("editing", editing);
        model.addAttribute("files", editing ? service.files(board.getBoardId()) : List.of());
        return "board/form";
    }
}
