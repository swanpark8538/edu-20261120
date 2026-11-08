package com.example.boardapp.service;

import com.example.boardapp.domain.Attachment;
import com.example.boardapp.domain.Board;
import com.example.boardapp.repository.AttachmentRepository;
import com.example.boardapp.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final AttachmentRepository attachmentRepository;

    /**
     * Base directory where uploaded files are stored. Configured via the
     * `Globals.fileStorePath` property.
     */
    @Value("${Globals.fileStorePath:./filestore}")
    private String fileStorePath;

    @Transactional(readOnly = true)
    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Board findById(String boardId) {
        return boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("Board not found"));
    }

    @Transactional
    public Board createBoard(Board board, List<MultipartFile> files) throws IOException {
        // generate board id (simple UUID based)
        board.setBoardId(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20));
        board.setCreatedAt(LocalDateTime.now());

        // handle files
        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                Attachment attachment = storeFile(file);
                attachment.setBoard(board);
                board.getAttachments().add(attachment);
            }
        }
        return boardRepository.save(board);
    }

    @Transactional
    public Board updateBoard(String boardId, Board updated, List<MultipartFile> files) throws IOException {
        Board existing = findById(boardId);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setWriter(updated.getWriter());

        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                Attachment attachment = storeFile(file);
                attachment.setBoard(existing);
                existing.getAttachments().add(attachment);
            }
        }
        return boardRepository.save(existing);
    }

    @Transactional
    public void deleteBoard(String boardId) {
        Board board = findById(boardId);
        // delete physical files
        for (Attachment att : board.getAttachments()) {
            try {
                Files.deleteIfExists(Path.of(att.getStoredPath()));
            } catch (IOException ignored) {
            }
        }
        boardRepository.delete(board);
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachId) {
        return attachmentRepository.findById(attachId).orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
    }

    private Attachment storeFile(MultipartFile file) throws IOException {
        // ensure directory exists
        Path dir = Path.of(fileStorePath);
        Files.createDirectories(dir);

        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + "_" + file.getOriginalFilename();
        Path target = dir.resolve(storedFileName);
        Files.copy(file.getInputStream(), target);

        return Attachment.builder()
                .fileName(file.getOriginalFilename())
                .storedPath(target.toAbsolutePath().toString())
                .fileSize(file.getSize())
                .uploadTime(LocalDateTime.now())
                .build();
    }
}
