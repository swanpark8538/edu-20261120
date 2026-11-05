package egovframework.example.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * DataSource (멀티 프로파일).
 *
 * 기본 활성 프로파일은 application.properties 의 spring.profiles.active=hsql 로 지정되어 있어
 * 별도 설정 없이 임베디드 HSQL DB 로 즉시 실행/테스트가 가능합니다.
 *
 * 운영 DB 로 전환하는 방법:
 *   1) src/main/resources/egovframework/egovProps/globals.properties 에서
 *      해당 DB 의 Globals.{db}.* 항목 주석을 해제하고 값을 채웁니다.
 *   2) application.properties 의 spring.profiles.active 값을 변경합니다.
 *      예) spring.profiles.active=oracle
 *   3) pom.xml 의 해당 JDBC 드라이버 의존성 주석을 해제합니다.
 *
 * 자격증명 주입 정책:
 *   본 클래스는 @Value("${Globals.{db}.UserName:}") / @Value("${Globals.{db}.Password:}")
 *   placeholder 로만 자격증명을 받습니다. 평문이 코드에 들어가지 않으며,
 *   기본 globals.properties 템플릿이 ${DB_<vendor>_PASSWORD:} env var override
 *   문법을 사용하도록 작성되어 있어 외부 secret manager (K8s Secret, Vault, CI
 *   secrets 등) 로 주입 가능합니다. 세부 가이드는 globals.properties 상단 주석을 참고하세요.
 *
 * 지원 프로파일: hsql(기본) / oracle / mysql / maria / postgres / tibero / altibase / cubrid / goldilocks
 */
@Configuration
public class EgovConfigDatasource {

	@Bean(name = "dataSource")
	@Profile({"default", "hsql"})
	public DataSource hsqlDataSource() {
		return new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.HSQL)
				.addScript("classpath:/db/deptdb.sql")
				.build();
	}

	@Bean(name = "dataSource")
	@Profile("oracle")
	public DataSource oracleDataSource(
			@Value("${Globals.oracle.DriverClassName:}") String driver,
			@Value("${Globals.oracle.Url:}") String url,
			@Value("${Globals.oracle.UserName:}") String username,
			@Value("${Globals.oracle.Password:}") String password) {
		return buildBasicDataSource(driver, url, username, password);
	}

	@Bean(name = "dataSource")
	@Profile("mysql")
	public DataSource mysqlDataSource(
			@Value("${Globals.mysql.DriverClassName:}") String driver,
			@Value("${Globals.mysql.Url:}") String url,
			@Value("${Globals.mysql.UserName:}") String username,
			@Value("${Globals.mysql.Password:}") String password) {
		return buildBasicDataSource(driver, url, username, password);
	}

	@Bean(name = "dataSource")
	@Profile("maria")
	public DataSource mariaDataSource(
			@Value("${Globals.maria.DriverClassName:}") String driver,
			@Value("${Globals.maria.Url:}") String url,
			@Value("${Globals.maria.UserName:}") String username,
			@Value("${Globals.maria.Password:}") String password) {
		return buildBasicDataSource(driver, url, username, password);
	}

	@Bean(name = "dataSource")
	@Profile("postgres")
	public DataSource postgresDataSource(
			@Value("${Globals.postgres.DriverClassName:}") String driver,
			@Value("${Globals.postgres.Url:}") String url,
			@Value("${Globals.postgres.UserName:}") String username,
			@Value("${Globals.postgres.Password:}") String password) {
		return buildBasicDataSource(driver, url, username, password);
	}

	@Bean(name = "dataSource")
	@Profile("tibero")
	public DataSource tiberoDataSource(
			@Value("${Globals.tibero.DriverClassName:}") String driver,
			@Value("${Globals.tibero.Url:}") String url,
			@Value("${Globals.tibero.UserName:}") String username,
			@Value("${Globals.tibero.Password:}") String password) {
		return buildBasicDataSource(driver, url, username, password);
	}

	@Bean(name = "dataSource")
	@Profile("altibase")
	public DataSource altibaseDataSource(
			@Value("${Globals.altibase.DriverClassName:}") String driver,
			@Value("${Globals.altibase.Url:}") String url,
			@Value("${Globals.altibase.UserName:}") String username,
			@Value("${Globals.altibase.Password:}") String password) {
		BasicDataSource ds = buildBasicDataSource(driver, url, username, password);
		ds.setValidationQuery("select 1");
		ds.setTestWhileIdle(true);
		return ds;
	}

	@Bean(name = "dataSource")
	@Profile("cubrid")
	public DataSource cubridDataSource(
			@Value("${Globals.cubrid.DriverClassName:}") String driver,
			@Value("${Globals.cubrid.Url:}") String url,
			@Value("${Globals.cubrid.UserName:}") String username,
			@Value("${Globals.cubrid.Password:}") String password) {
		BasicDataSource ds = buildBasicDataSource(driver, url, username, password);
		ds.setValidationQuery("select 1");
		ds.setTestOnBorrow(false);
		return ds;
	}

	@Bean(name = "dataSource")
	@Profile("goldilocks")
	public DataSource goldilocksDataSource(
			@Value("${Globals.goldilocks.DriverClassName:}") String driver,
			@Value("${Globals.goldilocks.Url:}") String url,
			@Value("${Globals.goldilocks.UserName:}") String username,
			@Value("${Globals.goldilocks.Password:}") String password) {
		return buildBasicDataSource(driver, url, username, password);
	}

	private BasicDataSource buildBasicDataSource(String driver, String url, String username, String password) {
		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName(driver);
		ds.setUrl(url);
		ds.setUsername(username);
		ds.setPassword(password);
		return ds;
	}
}
