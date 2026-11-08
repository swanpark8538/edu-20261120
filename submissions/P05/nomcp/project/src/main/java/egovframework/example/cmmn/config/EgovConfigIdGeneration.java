package egovframework.example.cmmn.config;

import javax.sql.DataSource;

import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 실행환경 ID Generation: 게시물 ID 와 첨부 묶음 ID 를 IDS 테이블에서 채번한다. */
@Configuration
public class EgovConfigIdGeneration {

    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl boardIdGnrService(DataSource dataSource) {
        return idGnrService(dataSource, "BOARD_ID", "BOARD_", 14);
    }

    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl fileIdGnrService(DataSource dataSource) {
        return idGnrService(dataSource, "FILE_ID", "FILE_", 15);
    }

    private EgovTableIdGnrServiceImpl idGnrService(DataSource dataSource, String tableName, String prefix, int cipers) {
        EgovTableIdGnrServiceImpl service = new EgovTableIdGnrServiceImpl();
        service.setDataSource(dataSource);
        service.setStrategy(new EgovIdGnrStrategyImpl(prefix, cipers, '0'));
        service.setBlockSize(10);
        service.setTable("IDS");
        service.setTableName(tableName);
        return service;
    }
}
