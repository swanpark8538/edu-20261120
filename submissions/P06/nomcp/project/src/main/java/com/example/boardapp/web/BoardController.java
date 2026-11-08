package com.example.boardapp.web;

import com.example.boardapp.domain.Attachment;
import com.example.boardapp.domain.Board;
import com.example.boardapp.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /** List page */
    @GetMapping("/egovBoardList.do")
    public String list(@RequestParam(value = "pageIndex", defaultValue = "0") int pageIndex,
                       @RequestParam(value = "searchCondition", required = false) String searchCondition,
                       @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
                       Model model) {
        // Simple implementation ignoring paging and search for brevity.
        List<Board> boards = boardService.findAll();
        model.addAttribute("boards", boards);
        return "board/list";
    }

    /** Show create form */
    @PostMapping("/addBoardView.do")
    public String addView() {
        return "board/form";
    }

    /** Process create */
    @PostMapping(value = "/addBoard.do", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String addBoard(@ModelAttribute Board board,
                           @RequestParam(value = "file_1", required = false) List<MultipartFile> files) throws IOException {
        boardService.createBoard(board, files);
        return "redirect:/egovBoardList.do";
    }

    /** Show update form */
    @PostMapping("/updateBoardView.do")
    public String updateView(@RequestParam("boardId") String boardId, Model model) {
        Board board = boardService.findById(boardId);
        model.addAttribute("board", board);
        return "board/form";
    }

    /** Process update */
    @PostMapping(value = "/updateBoard.do", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateBoard(@RequestParam("boardId") String boardId,
                              @ModelAttribute Board board,
                              @RequestParam(value = "file_1", required = false) List<MultipartFile> files) throws IOException {
        boardService.updateBoard(boardId, board, files);
        return "redirect:/egovBoardList.do";
    }

    /** Delete board */
    @PostMapping("/deleteBoard.do")
    public String deleteBoard(@RequestParam("boardId") String boardId) {
        boardService.deleteBoard(boardId);
        return "redirect:/egovBoardList.do";
    }

    /** Download attachment */
    @GetMapping("/attachments/{attachId}/download")
    public ResponseEntity<PathResource> download(@PathVariable("attachId") Long attachId) {
        Attachment att = boardService.getAttachment(attachId);
        Path path = Path.of(att.getStoredPath());
        PathResource resource = new PathResource(path);
        String fileName = StringUtils.getFilename(att.getFileName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(att.getFileSize())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
