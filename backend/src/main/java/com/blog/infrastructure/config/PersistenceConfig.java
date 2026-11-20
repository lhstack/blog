package com.blog.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.blog.infrastructure.persistence.typehandler.LocalDateTimeTypeHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 数据源与 MyBatis-Plus 配置。
 * <p>
 * SQLite 关键 pragma：WAL 允许读写并行、busy_timeout 缓解写锁竞争、foreign_keys 启用级联。
 * 通过连接初始化 SQL 与数据源属性下发，保证每个新连接都生效。
 * <p>
 * 必须使用 {@link MybatisSqlSessionFactoryBean}（而非原生 SqlSessionFactoryBean），
 * 否则 MyBatis-Plus 的 BaseMapper 通用方法不会被注册。
 */
@Configuration
public class PersistenceConfig {

    @Bean
    public DataSource dataSource(@Value("${blog.data-dir:./data}") String dataDir) throws IOException {
        Path dir = Path.of(dataDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path dbFile = dir.resolve("blog.db");

        HikariConfig config = new HikariConfig();
        config.setPoolName("blog-sqlite-pool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile);
        // SQLite 同一时刻仅一个写者，池不宜过大，否则只会放大写锁竞争
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        config.addDataSourceProperty("synchronous", "NORMAL");
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               MybatisPlusInterceptor interceptor) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeHandlers(new LocalDateTimeTypeHandler());
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml"));
        factory.setPlugins(interceptor);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultStatementTimeout(10);
        // 编译时保留参数名，映射语句可直接使用 #{name}
        configuration.setUseActualParamName(true);
        factory.setConfiguration(configuration);
        return factory.getObject();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.SQLITE);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
