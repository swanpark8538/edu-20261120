package egovframework.example.cmmn.file.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import egovframework.example.cmmn.file.service.EgovFileMngService;
import egovframework.example.cmmn.file.service.FileVO;

/**
 * 첨부파일 내려받기 컨트롤러. 파일은 식별자로만 찾고 항상 다운로드로 응답한다.
 */
@Controller
public class EgovFileDownloadController {

    @jakarta.annotation.Resource(name = "fileMngService")
    private EgovFileMngService fileMngService;

    @GetMapping("/downloadFile.do")
    public ResponseEntity<Resource> download(@RequestParam("atchFileId") String atchFileId,
            @RequestParam("fileSn") int fileSn) throws Exception {
        FileVO file = fileMngService.selectFile(atchFileId, fileSn);
        Path path = fileMngService.resolveStoredFile(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentHeader(file.getOrignlFileNm()))
                .body(new FileSystemResource(path));
    }

    /** ASCII 파일명은 일반 filename 으로, 그 외에는 UTF-8 인코딩으로 내보낸다. */
    private String attachmentHeader(String fileName) {
        ContentDisposition.Builder builder = ContentDisposition.attachment();
        if (StandardCharsets.US_ASCII.newEncoder().canEncode(fileName)) {
            builder.filename(fileName);
        } else {
            builder.filename(fileName, StandardCharsets.UTF_8);
        }
        return builder.build().toString();
    }
}
