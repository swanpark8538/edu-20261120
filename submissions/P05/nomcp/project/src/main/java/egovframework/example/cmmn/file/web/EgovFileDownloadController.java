package egovframework.example.cmmn.file.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import egovframework.example.cmmn.file.service.EgovFileMngService;
import egovframework.example.cmmn.file.service.FileVO;

@Controller
public class EgovFileDownloadController {

    @jakarta.annotation.Resource(name = "fileMngService")
    private EgovFileMngService fileMngService;

    /** DB 에 등록된 첨부만 식별자로 내려준다. 경로나 파일명은 요청에서 받지 않는다. */
    @GetMapping("/fileDown.do")
    public ResponseEntity<Resource> fileDown(@RequestParam String atchFileId, @RequestParam int fileSn) throws Exception {
        FileVO file = fileMngService.selectFile(atchFileId, fileSn);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path path = fileMngService.resolveStoredFile(file);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getOrignlFileNm(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(path));
    }
}
