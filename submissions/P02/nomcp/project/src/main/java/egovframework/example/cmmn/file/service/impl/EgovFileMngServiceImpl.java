package egovframework.example.cmmn.file.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import egovframework.example.cmmn.file.service.EgovFileMngService;
import egovframework.example.cmmn.file.service.FileVO;
import jakarta.annotation.Resource;

/**
 * 첨부파일 관리 서비스 구현.
 * 파일은 웹 경로 밖의 저장소에 추측할 수 없는 이름과 확장자 없는 형태로 저장한다.
 */
@Service("fileMngService")
@Transactional(rollbackFor = Exception.class)
public class EgovFileMngServiceImpl extends EgovAbstractServiceImpl implements EgovFileMngService {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    /** 이중 확장자(예: shell.jsp.jpg)로 파일 이름 중간에 있어도 허용하지 않는 서버 실행형 확장자. */
    private static final Set<String> EXECUTABLE_EXTENSIONS = Set.of(
            "jsp", "jspx", "jsw", "jsv", "jspf", "war", "php", "php3", "php4", "php5", "phtml",
            "asp", "aspx", "asa", "ascx", "ashx", "exe", "dll", "bat", "cmd", "sh", "bash", "cgi", "html", "htm");

    @Resource(name = "fileManageDAO")
    private FileManageDAO fileManageDAO;

    @Resource(name = "fileIdGnrService")
    private EgovIdGnrService fileIdGnrService;

    @Value("${Globals.fileStorePath}")
    private String fileStorePath;

    @Value("#{'${Globals.fileAllowedExtensions}'.toLowerCase().split(',')}")
    private Set<String> allowedExtensions;

    @Value("${Globals.fileMaxCount}")
    private int maxFileCount;

    @Override
    public String saveFiles(List<MultipartFile> files, String atchFileId) throws Exception {
        List<MultipartFile> uploads = selectedFiles(files);
        if (uploads.isEmpty()) {
            return atchFileId;
        }

        List<String> originalNames = new ArrayList<>();
        for (MultipartFile upload : uploads) {
            originalNames.add(validatedFileName(upload.getOriginalFilename()));
        }

        int lastFileSn = atchFileId == null ? -1 : fileManageDAO.selectMaxFileSn(atchFileId);
        if (lastFileSn + 1 + uploads.size() > maxFileCount) {
            throw processException("fail.file.tooMany", new String[] {String.valueOf(maxFileCount)});
        }

        String targetId = atchFileId == null ? fileIdGnrService.getNextStringId() : atchFileId;
        Path root = storeRoot();
        List<Path> written = new ArrayList<>();
        deleteWhenRolledBack(written);

        try {
            Files.createDirectories(root);
            for (int i = 0; i < uploads.size(); i++) {
                MultipartFile upload = uploads.get(i);
                String storedName = UUID.randomUUID().toString().replace("-", "");
                Path target = insideRoot(root, storedName);
                written.add(target);
                upload.transferTo(target);

                FileVO file = new FileVO();
                file.setAtchFileId(targetId);
                file.setFileSn(lastFileSn + 1 + i);
                file.setStreFileNm(storedName);
                file.setOrignlFileNm(originalNames.get(i));
                file.setFileExtsn(extensionOf(originalNames.get(i)));
                file.setFileSize(upload.getSize());
                fileManageDAO.insertFile(file);
            }
        } catch (IOException e) {
            throw processException("fail.file.store", e);
        }
        return targetId;
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<FileVO> selectFileList(String atchFileId) {
        if (atchFileId == null) {
            return List.of();
        }
        return fileManageDAO.selectFileList(atchFileId);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public FileVO selectFile(String atchFileId, int fileSn) throws Exception {
        FileVO condition = new FileVO();
        condition.setAtchFileId(atchFileId);
        condition.setFileSn(fileSn);
        FileVO file = fileManageDAO.selectFile(condition);
        if (file == null) {
            throw processException("fail.file.notFound");
        }
        return file;
    }

    @Override
    public Path resolveStoredFile(FileVO file) throws Exception {
        Path path = insideRoot(storeRoot(), file.getStreFileNm());
        if (!Files.isRegularFile(path)) {
            throw processException("fail.file.notFound");
        }
        return path;
    }

    @Override
    public void deleteFiles(String atchFileId) throws Exception {
        if (atchFileId == null) {
            return;
        }
        Path root = storeRoot();
        List<Path> paths = new ArrayList<>();
        for (FileVO file : fileManageDAO.selectFileList(atchFileId)) {
            paths.add(insideRoot(root, file.getStreFileNm()));
        }
        fileManageDAO.deleteFiles(atchFileId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paths.forEach(EgovFileMngServiceImpl.this::deleteQuietly);
            }
        });
    }

    /** 파일을 선택하지 않은 입력란은 파일명이 비어 있는 part 로 전송되므로 제외한다. */
    private List<MultipartFile> selectedFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                .collect(Collectors.toList());
    }

    private String validatedFileName(String submittedName) throws Exception {
        String name = submittedName.substring(Math.max(submittedName.lastIndexOf('/'), submittedName.lastIndexOf('\\')) + 1);
        boolean hasControlChar = submittedName.chars().anyMatch(Character::isISOControl);
        if (hasControlChar || name.isBlank() || name.length() > MAX_FILE_NAME_LENGTH) {
            throw processException("fail.file.name");
        }

        String[] segments = name.toLowerCase(Locale.ROOT).split("\\.", -1);
        String extension = segments[segments.length - 1];
        if (segments.length < 2 || !allowedExtensions.contains(extension)) {
            throw processException("fail.file.extension");
        }
        for (String segment : segments) {
            if (EXECUTABLE_EXTENSIONS.contains(segment.trim())) {
                throw processException("fail.file.extension");
            }
        }
        return name;
    }

    private String extensionOf(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private Path storeRoot() {
        return Paths.get(fileStorePath).toAbsolutePath().normalize();
    }

    private Path insideRoot(Path root, String storedName) throws Exception {
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw processException("fail.file.notFound");
        }
        return path;
    }

    private void deleteWhenRolledBack(List<Path> written) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    written.forEach(EgovFileMngServiceImpl.this::deleteQuietly);
                }
            }
        });
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            egovLogger.warn("stored file could not be deleted: {}", path.getFileName());
        }
    }
}
