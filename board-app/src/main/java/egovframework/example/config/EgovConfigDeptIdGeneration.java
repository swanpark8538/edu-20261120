package egovframework.example.config;

import javax.sql.DataSource;

import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 엔티티별 ID 생성기 설정. 클래스 이름과 두 @Bean 메서드 모두 엔티티명 prefix 를
 * 포함하므로 한 프로젝트에 여러 엔티티가 있어도 빈 이름이 충돌하지 않는다.
 * egov_add_entity 가 새 엔티티를 추가할 때 같은 패턴의 새 파일을 만든다.
 */
@Configuration
public class EgovConfigDeptIdGeneration {

	@Bean
	public EgovIdGnrStrategyImpl mixPrefixDept() {
		EgovIdGnrStrategyImpl egovIdGnrStrategyImpl = new EgovIdGnrStrategyImpl();
		egovIdGnrStrategyImpl.setPrefix("DEPT-");
		egovIdGnrStrategyImpl.setCipers(5);
		egovIdGnrStrategyImpl.setFillChar('0');
		return egovIdGnrStrategyImpl;
	}

	@Bean(name = "egovDeptIdGnrService", destroyMethod="destroy")
	public EgovTableIdGnrServiceImpl egovDeptIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
		EgovTableIdGnrServiceImpl egovTableIdGnrServiceImpl = new EgovTableIdGnrServiceImpl();
		egovTableIdGnrServiceImpl.setDataSource(dataSource);
		egovTableIdGnrServiceImpl.setStrategy(mixPrefixDept());
		egovTableIdGnrServiceImpl.setBlockSize(10);
		egovTableIdGnrServiceImpl.setTable("IDS");
		egovTableIdGnrServiceImpl.setTableName("DEPT");
		return egovTableIdGnrServiceImpl;
	}

}
