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

@Service("fileMngService")
@Transactional(rollbackFor = Exception.class)
public class EgovFileMngServiceImpl extends EgovAbstractServiceImpl implements EgovFileMngService {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    @Resource(name = "fileDAO")
    private FileDAO fileDAO;

    @Resource(name = "fileIdGnrService")
    private EgovIdGnrService fileIdGnrService;

    @Value("${Globals.fileStorePath}")
    private String fileStorePath;

    @Value("${Globals.fileUpload.maxFileSize}")
    private long maxFileSize;

    @Value("${Globals.fileUpload.maxFileCount}")
    private int maxFileCount;

    @Value("${Globals.fileUpload.allowedExtensions}")
    private Set<String> allowedExtensions;

    @Override
    public String saveFiles(List<MultipartFile> files, String atchFileId) throws Exception {
        List<MultipartFile> uploads = files == null ? List.of() : files.stream().filter(file -> !file.isEmpty()).toList();
        if (uploads.isEmpty()) {
            return atchFileId;
        }
        if (uploads.size() > maxFileCount) {
            throw processException("fail.file.count");
        }

        List<String> fileNames = new ArrayList<>();
        for (MultipartFile upload : uploads) {
            fileNames.add(validate(upload));
        }

        String groupId = atchFileId != null ? atchFileId : fileIdGnrService.getNextStringId();
        int fileSn = fileDAO.selectNextFileSn(groupId);
        Path storeRoot = storeRoot();
        Files.createDirectories(storeRoot);

        List<Path> stored = new ArrayList<>();
        removeOnRollback(stored);
        for (int i = 0; i < uploads.size(); i++) {
            // 저장 이름은 확장자 없는 난수로 만들어 업로드한 파일이 실행되거나 경로를 조작하지 못하게 한다.
            String storedName = UUID.randomUUID().toString();
            Path target = storeRoot.resolve(storedName);
            stored.add(target);
            uploads.get(i).transferTo(target);

            FileVO file = new FileVO();
            file.setAtchFileId(groupId);
            file.setFileSn(fileSn + i);
            file.setOrignlFileNm(fileNames.get(i));
            file.setStreFileNm(storedName);
            file.setFileSize(uploads.get(i).getSize());
            fileDAO.insertFile(file);
        }
        return groupId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVO> selectFileList(String atchFileId) throws Exception {
        return atchFileId == null ? List.of() : fileDAO.selectFileList(atchFileId);
    }

    @Override
    @Transactional(readOnly = true)
    public FileVO selectFile(String atchFileId, int fileSn) throws Exception {
        FileVO key = new FileVO();
        key.setAtchFileId(atchFileId);
        key.setFileSn(fileSn);
        return fileDAO.selectFile(key);
    }

    @Override
    public Path resolveStoredFile(FileVO file) throws Exception {
        Path storeRoot = storeRoot();
        Path path = storeRoot.resolve(file.getStreFileNm()).normalize();
        if (!path.startsWith(storeRoot)) {
            throw processException("fail.file.name");
        }
        return path;
    }

    @Override
    public void deleteFiles(String atchFileId) throws Exception {
        if (atchFileId == null) {
            return;
        }
        List<Path> paths = new ArrayList<>();
        for (FileVO file : fileDAO.selectFileList(atchFileId)) {
            paths.add(resolveStoredFile(file));
        }
        fileDAO.deleteFiles(atchFileId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(paths);
            }
        });
    }

    /** 검증을 통과한 원본 파일 이름(경로 제거)을 돌려준다. */
    private String validate(MultipartFile upload) throws Exception {
        String original = upload.getOriginalFilename();
        if (original == null) {
            throw processException("fail.file.name");
        }
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).strip();
        if (name.isEmpty() || name.length() > MAX_FILE_NAME_LENGTH || name.chars().anyMatch(Character::isISOControl)) {
            throw processException("fail.file.name");
        }
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw processException("fail.file.extension");
        }
        if (upload.getSize() > maxFileSize) {
            throw processException("fail.file.size");
        }
        return name;
    }

    private Path storeRoot() {
        return Paths.get(fileStorePath).toAbsolutePath().normalize();
    }

    private void removeOnRollback(List<Path> stored) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(stored);
                }
            }
        });
    }

    private void deleteQuietly(List<Path> paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                egovLogger.warn("첨부파일 삭제 실패: {}", path.getFileName(), e);
            }
        }
    }
}
