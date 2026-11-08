package egovframework.board.service.impl;

import egovframework.board.service.AttachmentVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FileStorage {
    private static final Logger LOG = LoggerFactory.getLogger(FileStorage.class);
    private static final Set<String> ALLOWED = Set.of("txt", "csv", "pdf", "png", "jpg", "jpeg", "gif", "zip", "docx", "xlsx", "pptx", "hwp", "hwpx", "doc", "xls", "ppt");
    private final Path root;

    public FileStorage(@Value("${Globals.fileStorePath}") String directory) throws IOException {
        root = Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new IOException("Upload directory must not be a symlink");
    }

    public void validate(List<MultipartFile> uploads, int existingCount) {
        if (uploads.size() + existingCount > 10) bad("첨부파일은 게시물당 최대 10개입니다.");
        for (MultipartFile file : uploads) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank() || name.length() > 255 || name.contains("/") || name.contains("\\") || name.codePoints().anyMatch(c -> Character.isISOControl(c))) bad("파일 이름이 올바르지 않습니다.");
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED.contains(ext)) bad("허용되지 않는 파일 형식입니다. 첨부 안내를 확인해 주세요.");
            if (file.getSize() > 10 * 1024 * 1024) bad("파일 하나의 크기는 10MB 이하여야 합니다.");
        }
    }

    public AttachmentVO store(String boardId, MultipartFile upload) throws IOException {
        String id = UUID.randomUUID().toString();
        Path path = resolve(id);
        // Rollback removes every newly created file, including partially written files.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) remove(path);
            }
        });
        try (InputStream input = upload.getInputStream()) {
            Files.copy(input, path); // no overwrite; original filenames never become filesystem paths
        }
        AttachmentVO file = new AttachmentVO();
        file.setFileId(id); file.setBoardId(boardId); file.setOriginalName(upload.getOriginalFilename());
        file.setStoredName(id); file.setFileSize(upload.getSize());
        return file;
    }

    public Path resolve(String storedName) {
        if (storedName == null || !storedName.matches("[a-f0-9-]{36}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path path = root.resolve(storedName).normalize();
        if (!path.getParent().equals(root) || Files.isSymbolicLink(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return path;
    }

    public void deleteAfterCommit(List<AttachmentVO> files) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { files.forEach(file -> remove(resolve(file.getStoredName()))); }
        });
    }

    private void remove(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { LOG.warn("첨부파일 정리 실패: {}", path.getFileName(), e); }
    }
    private static void bad(String message) { throw new IllegalArgumentException(message); }
}
