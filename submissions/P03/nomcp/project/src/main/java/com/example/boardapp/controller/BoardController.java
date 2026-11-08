package com.example.boardapp.controller;

import com.example.boardapp.config.Globals;
import com.example.boardapp.entity.Attachment;
import com.example.boardapp.entity.Board;
import com.example.boardapp.repository.AttachmentRepository;
import com.example.boardapp.repository.BoardRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.PathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class BoardController {

    private final BoardRepository boardRepository;
    private final AttachmentRepository attachmentRepository;
    private final Globals globals;

    public BoardController(BoardRepository boardRepository, AttachmentRepository attachmentRepository, Globals globals) {
        this.boardRepository = boardRepository;
        this.attachmentRepository = attachmentRepository;
        this.globals = globals;
    }

    /** 목록 */
    @GetMapping("/egovBoardList.do")
    public String list(@RequestParam(defaultValue = "1") int pageIndex,
                       @RequestParam(required = false) Integer searchCondition,
                       @RequestParam(required = false) String searchKeyword,
                       Model model) {
        Page<Board> page = boardRepository.findAll(PageRequest.of(pageIndex - 1, 10));
        model.addAttribute("page", page);
        return "board/list";
    }

    /** 등록 화면 */
    @PostMapping("/addBoardView.do")
    public String addView() {
        return "board/add";
    }

    /** 등록 처리 */
    @PostMapping("/addBoard.do")
    public String addBoard(@RequestParam String title,
                           @RequestParam String content,
                           @RequestParam String writer,
                           @RequestParam(value = "file_1", required = false) List<MultipartFile> files) throws IOException {
        Board board = new Board();
        board.setBoardId(UUID.randomUUID().toString());
        board.setTitle(title);
        board.setContent(content);
        board.setWriter(writer);
        saveAttachments(board, files);
        boardRepository.save(board);
        return "redirect:/egovBoardList.do";
    }

    /** 수정 화면 */
    @PostMapping("/updateBoardView.do")
    public String updateView(@RequestParam String boardId, Model model) {
        Optional<Board> opt = boardRepository.findById(boardId);
        if (opt.isPresent()) {
            model.addAttribute("board", opt.get());
            return "board/edit";
        } else {
            return "redirect:/egovBoardList.do";
        }
    }

    /** 수정 처리 */
    @PostMapping("/updateBoard.do")
    public String updateBoard(@RequestParam String boardId,
                              @RequestParam String title,
                              @RequestParam String content,
                              @RequestParam String writer,
                              @RequestParam(required = false) String atchFileId,
                              @RequestParam(value = "file_1", required = false) List<MultipartFile> files) throws IOException {
        Board board = boardRepository.findById(boardId).orElseThrow();
        board.setTitle(title);
        board.setContent(content);
        board.setWriter(writer);
        // 기존 첨부는 유지, 새 파일 추가
        saveAttachments(board, files);
        boardRepository.save(board);
        return "redirect:/egovBoardList.do";
    }

    /** 삭제 */
    @PostMapping("/deleteBoard.do")
    public String deleteBoard(@RequestParam String boardId) {
        boardRepository.deleteById(boardId);
        return "redirect:/egovBoardList.do";
    }

    /** 첨부 파일 다운로드 */
    @GetMapping("/attachments/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Attachment attachment = attachmentRepository.findById(id).orElseThrow();
        Path file = Path.of(attachment.getStoredPath());
        if (Files.exists(file)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + attachment.getFileName() + "\"");
            Files.copy(file, response.getOutputStream());
            response.flushBuffer();
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void saveAttachments(Board board, List<MultipartFile> files) throws IOException {
        if (files == null) return;
        Path storeDir = Path.of(globals.getFileStorePath());
        Files.createDirectories(storeDir);
        for (MultipartFile multipartFile : files) {
            if (multipartFile.isEmpty()) continue;
            String original = StringUtils.cleanPath(multipartFile.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + "_" + original;
            Path target = storeDir.resolve(storedFileName);
            try (var input = multipartFile.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            Attachment att = new Attachment();
            att.setFileName(original);
            att.setStoredPath(target.toAbsolutePath().toString());
            att.setSize(multipartFile.getSize());
            board.addAttachment(att);
        }
    }
}
