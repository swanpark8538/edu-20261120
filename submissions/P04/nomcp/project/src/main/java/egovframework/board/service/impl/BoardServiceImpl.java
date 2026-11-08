package egovframework.board.service.impl;

import egovframework.board.service.*;
import java.util.*;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BoardServiceImpl extends EgovAbstractServiceImpl implements BoardService {
    private final BoardDAO dao;
    private final FileStorage storage;
    private final EgovIdGnrService ids;
    public BoardServiceImpl(BoardDAO dao, FileStorage storage, @Qualifier("boardIdGnrService") EgovIdGnrService ids) {
        this.dao = dao; this.storage = storage; this.ids = ids;
    }
    public List<BoardVO> selectList(BoardSearchVO s) { return dao.findAll(s); }
    public int count(BoardSearchVO s) { return dao.count(s); }
    public BoardVO select(String id) {
        if (id == null || !id.matches("BOARD_[0-9]{14}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        BoardVO board = dao.find(id);
        if (board == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return board;
    }
    public List<AttachmentVO> files(String id) { return dao.files(id); }
    public AttachmentVO file(String id) {
        if (id == null || !id.matches("[a-f0-9-]{36}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        AttachmentVO file = dao.file(id);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return file;
    }
    @Transactional(rollbackFor = Exception.class)
    public String insert(BoardVO board, List<MultipartFile> uploads) throws Exception {
        List<MultipartFile> actual = actualFiles(uploads);
        storage.validate(actual, 0);
        board.setBoardId("BOARD_" + String.format(Locale.ROOT, "%014d", ids.getNextLongId()));
        board.setAtchFileId(UUID.randomUUID().toString());
        dao.insertBoard(board);
        for (MultipartFile upload : actual) dao.insertFile(storage.store(board.getBoardId(), upload));
        return board.getBoardId();
    }
    @Transactional(rollbackFor = Exception.class)
    public void update(BoardVO board, List<MultipartFile> uploads) throws Exception {
        lock(board.getBoardId());
        BoardVO existing = select(board.getBoardId());
        if (board.getAtchFileId() != null && !board.getAtchFileId().isBlank() && !existing.getAtchFileId().equals(board.getAtchFileId())) {
            throw new IllegalArgumentException("첨부파일 묶음이 해당 게시물과 일치하지 않습니다.");
        }
        List<MultipartFile> actual = actualFiles(uploads);
        storage.validate(actual, existing.getFileCount());
        board.setAtchFileId(existing.getAtchFileId());
        dao.updateBoard(board);
        for (MultipartFile upload : actual) dao.insertFile(storage.store(board.getBoardId(), upload));
    }
    @Transactional
    public void delete(String id) {
        lock(id);
        List<AttachmentVO> files = dao.files(id);
        dao.deleteBoard(id);
        storage.deleteAfterCommit(files);
    }
    private List<MultipartFile> actualFiles(List<MultipartFile> uploads) {
        return uploads == null ? List.of() : uploads.stream().filter(f -> !f.isEmpty()).toList();
    }
    private void lock(String id) {
        if (id == null || !id.matches("BOARD_[0-9]{14}") || dao.lock(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
