package egovframework.example.cmmn.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** EgovAbstractMapper 가 이름으로 주입받는 sqlSession 을 구성한다. */
@org.springframework.context.annotation.Configuration
public class EgovConfigMapper {

    @Bean(name = "sqlSession")
    public SqlSessionFactoryBean sqlSession(DataSource dataSource) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:/egovframework/mapper/**/*_SQL.xml"));
        return factory;
    }
}
