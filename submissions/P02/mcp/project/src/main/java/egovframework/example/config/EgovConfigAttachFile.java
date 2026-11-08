package egovframework.example.config;

import javax.sql.DataSource;

import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 첨부파일(atchFileId) ID 채번 Bean 설정.
 *
 * 전역 PK 채번 Bean(`egovIdGnrService`) 과 분리된 별도 채번 — prefix("FILE_") + cipers(15) =
 * 정확히 20자, COMTNFILE.ATCH_FILE_ID 의 CHAR(20) PK 와 길이 일치.
 *
 * 이 분리 덕분에 다른 도메인의 시퀀스를 잠식하지 않고 첨부파일만의 독립 시퀀스 row
 * (COMTECOPSEQ.TABLE_NAME='FILE_ID') 가 관리된다.
 *
 * 자동 등록: `@Configuration` + base package component-scan 으로 자동 발견.
 * (EgovConfigWeb 의 @Import 명시 등록 불필요 — base package 가 동일하면 됨.)
 */
@Configuration
public class EgovConfigAttachFile {

	@Bean(name = "egovFileIdGnrService", destroyMethod = "destroy")
	public EgovTableIdGnrServiceImpl egovFileIdGnrService(DataSource dataSource) throws Exception {
		EgovTableIdGnrServiceImpl bean = new EgovTableIdGnrServiceImpl();
		bean.setDataSource(dataSource);
		bean.setStrategy(fileStrategy());
		bean.setBlockSize(10);
		bean.setTable("COMTECOPSEQ");
		bean.setTableName("FILE_ID");
		return bean;
	}

	@Bean(name = "fileStrategy")
	public EgovIdGnrStrategyImpl fileStrategy() {
		EgovIdGnrStrategyImpl strategy = new EgovIdGnrStrategyImpl();
		strategy.setPrefix("FILE_");
		strategy.setCipers(15);
		strategy.setFillChar('0');
		return strategy;
	}
}
