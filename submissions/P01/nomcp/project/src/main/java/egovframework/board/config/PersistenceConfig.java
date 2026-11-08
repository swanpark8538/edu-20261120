package egovframework.board.config;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
@Configuration
public class PersistenceConfig {
 @Bean public org.springframework.context.MessageSource messageSource() {
  var source=new org.springframework.context.support.ReloadableResourceBundleMessageSource();
  source.setBasenames("classpath:org/egovframe/rte/fdl/idgnr/messages/idgnr", "classpath:messages/message-common");
  source.setDefaultEncoding("UTF-8");return source;
 }
 @Bean public org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace leaveaTrace() {
  var trace=new org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace();
  var manager=new org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager();
  manager.setReqExpMatcher(new org.springframework.util.AntPathMatcher());
  manager.setPatterns(new String[]{"*"});
  manager.setHandlers(new org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler[]{new org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler()});
  trace.setTraceHandlerServices(new org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService[]{manager}); return trace;
 }
 @Bean(name={"sqlSession", "sqlSessionFactory"}) public SqlSessionFactory sqlSessionFactory(DataSource ds) throws Exception {
  var bean = new SqlSessionFactoryBean(); bean.setDataSource(ds);
  var config = new org.apache.ibatis.session.Configuration(); config.setMapUnderscoreToCamelCase(true);
  bean.setConfiguration(config);
  bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
  return bean.getObject();
 }
 @Bean public EgovTableIdGnrServiceImpl boardIdGnrService(DataSource ds) {
  var service = new EgovTableIdGnrServiceImpl(); service.setDataSource(ds);
  service.setTable("IDS"); service.setTableName("BOARD"); service.setBlockSize(1); return service;
 }
}
