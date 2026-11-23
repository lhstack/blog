package com.blog.infrastructure.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.blog.infrastructure.persistence.typehandler.LocalDateTimeTypeHandler;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SqlInitializationProperties.class)
public class PersistenceConfig {

    @Bean
    DatabaseEngine databaseEngine(@Value("${blog.database.type:sqlite}") String type) {
        return DatabaseEngine.select(type);
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DatabaseEngine engine,
                                       @Value("${blog.data-dir:./data}") String dataDir,
                                       @Value("${blog.database.url:}") String url,
                                       @Value("${blog.database.username:}") String username,
                                       @Value("${blog.database.password:}") String password) throws IOException {
        if (engine != DatabaseEngine.SQLITE) {
            engine.validateUrl(url);
        }
        HikariDataSource source = new HikariDataSource();
        source.setDriverClassName(engine.driver);
        if (engine == DatabaseEngine.SQLITE) {
            configureSqlite(source, dataDir);
        } else {
            source.setJdbcUrl(url);
            source.setUsername(username);
            source.setPassword(password);
        }
        return source;
    }

    private static void configureSqlite(HikariDataSource source, String dataDir) throws IOException {
        Path dir = Path.of(dataDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        source.setJdbcUrl("jdbc:sqlite:" + dir.resolve("blog.db"));
        source.setConnectionInitSql("PRAGMA foreign_keys = ON");
        source.addDataSourceProperty("journal_mode", "WAL");
        source.addDataSourceProperty("busy_timeout", "5000");
        source.addDataSourceProperty("synchronous", "NORMAL");
    }

    @Bean
    SqlDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(
            DataSource dataSource, SqlInitializationProperties properties, DatabaseEngine engine) {
        var settings = SqlDataSourceScriptDatabaseInitializer.getSettings(properties);
        settings.setSchemaLocations(List.of("classpath:db/" + engine.schema));
        return new SqlDataSourceScriptDatabaseInitializer(dataSource, settings);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor interceptor,
                                               DatabaseEngine engine) throws Exception {
        // MyBatis-Plus 的 BaseMapper 通用方法依赖这个扩展工厂。
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeHandlers(new LocalDateTimeTypeHandler());
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml"));
        factory.setPlugins(interceptor);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setDatabaseId(engine.id);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultStatementTimeout(10);
        configuration.setUseActualParamName(true);
        factory.setConfiguration(configuration);
        return factory.getObject();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DatabaseEngine engine) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(engine.dialect);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
