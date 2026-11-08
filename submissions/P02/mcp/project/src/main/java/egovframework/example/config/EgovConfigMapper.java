package egovframework.example.config;

import java.io.IOException;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * MyBatis 통합 설정.
 *
 * 다중 엔티티 환경을 기본으로 가정한다:
 *   - `@MapperScan` 은 base 패키지 전체를 스캔하되 `@EgovMapper` 가 붙은 인터페이스만 빈으로 등록한다.
 *     향후 egov_add_entity 가 추가하는 새 모듈(예: egovframework.example.member.service.impl) 의 매퍼도 자동 등록된다.
 *   - `setMapperLocations` 는 `classpath*:` + `**` 와일드카드를 사용해 모든 entityModule 하위의 매퍼 XML 을 한 번에 로드한다.
 *   - typeAlias 는 `setTypeAliasesPackage` 로 base 패키지 하위 모든 VO 클래스를 자동 등록한다.
 *     (예: `BoardVO` → `boardVO`, `MemberDefaultVO` → `memberDefaultVO`)
 *     base 패키지 밖의 별칭(`egovMap`) 만 `setTypeAliases` 로 별도 등록한다.
 *
 * 따라서 egov_add_entity 가 추가하는 엔티티는 이 설정 파일을 손대지 않아도 즉시 인식된다.
 */
@Configuration
@MapperScan(annotationClass = EgovMapper.class, basePackages = "egovframework.example")
public class EgovConfigMapper {

	@Bean(name = "sqlSession")
	public SqlSessionFactoryBean sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws IOException {
		PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver();
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
		sqlSessionFactoryBean.setMapperLocations(pmrpr.getResources("classpath*:/egovframework/sqlmap/**/mappers/*.xml"));
		sqlSessionFactoryBean.setTypeAliasesPackage("egovframework.example");
		sqlSessionFactoryBean.setTypeAliases(new Class<?>[]{ EgovMap.class });

		// resultType="egovMap" 목록 조회 시 NULL 컬럼도 맵 키로 포함 (기본값 false)
		org.apache.ibatis.session.Configuration mybatisConfiguration = new org.apache.ibatis.session.Configuration();
		mybatisConfiguration.setCallSettersOnNulls(true);
		sqlSessionFactoryBean.setConfiguration(mybatisConfiguration);
				
		return sqlSessionFactoryBean;
	}

	@Bean(name = "sqlSessionTemplate")
	public SqlSessionTemplate sqlSession(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}

}
