package egovframework.example.config;

import java.io.IOException;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * MyBatis 설정. EgovAbstractMapper 가 "sqlSession" 이름의 빈을 사용한다.
 */
@org.springframework.context.annotation.Configuration
public class EgovConfigMapper {

    @Bean(name = "sqlSession")
    public SqlSessionFactoryBean sqlSession(DataSource dataSource) throws IOException {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(JdbcType.VARCHAR);

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:/egovframework/mapper/*_SQL.xml"));
        return factory;
    }
}
