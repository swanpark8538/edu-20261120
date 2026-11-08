package egovframework.example.config;

import javax.sql.DataSource;

import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 테이블 기반 ID Generation 서비스 설정. 번호는 IDS 테이블에서 관리한다.
 */
@Configuration
public class EgovConfigIdGeneration {

    private static final int BLOCK_SIZE = 10;

    @Bean(name = "boardIdGnrService", destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl boardIdGnrService(DataSource dataSource) {
        return tableIdGnrService(dataSource, "BOARD_ID", "BOARD_", 14);
    }

    @Bean(name = "fileIdGnrService", destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl fileIdGnrService(DataSource dataSource) {
        return tableIdGnrService(dataSource, "ATCH_FILE_ID", "FILE_", 15);
    }

    private EgovTableIdGnrServiceImpl tableIdGnrService(DataSource dataSource, String key, String prefix, int digits) {
        EgovTableIdGnrServiceImpl service = new EgovTableIdGnrServiceImpl();
        service.setDataSource(dataSource);
        service.setStrategy(new EgovIdGnrStrategyImpl(prefix, digits, '0'));
        service.setBlockSize(BLOCK_SIZE);
        service.setTable("IDS");
        service.setTableName(key);
        return service;
    }
}
