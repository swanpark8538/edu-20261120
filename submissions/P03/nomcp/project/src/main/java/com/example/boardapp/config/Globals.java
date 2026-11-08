package com.example.boardapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Holds global properties that can be overridden via command line arguments.
 * Example: java -jar board-app.jar --Globals.fileStorePath=/tmp/board-files
 */
@Configuration
public class Globals {

    /**
     * 파일이 저장되는 디렉터리 절대 경로. 기본값은 시스템 임시 디렉터리 하위 "board-files".
     */
    @Value("${Globals.fileStorePath:${java.io.tmpdir}/board-files}")
    private String fileStorePath;

    public String getFileStorePath() {
        return fileStorePath;
    }
}
