package com.blog.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;

import java.util.Arrays;

/** JDBC driver, MyBatis dialect and schema must agree for the lifetime of the application. */
enum DatabaseEngine {
    SQLITE("sqlite", "org.sqlite.JDBC", DbType.SQLITE, "schema.sql"),
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", DbType.MYSQL, "schema-mysql.sql"),
    POSTGRESQL("postgresql", "org.postgresql.Driver", DbType.POSTGRE_SQL, "schema-postgresql.sql");

    final String id;
    final String driver;
    final DbType dialect;
    final String schema;

    DatabaseEngine(String id, String driver, DbType dialect, String schema) {
        this.id = id;
        this.driver = driver;
        this.dialect = dialect;
        this.schema = schema;
    }

    static DatabaseEngine select(String type) {
        return Arrays.stream(values()).filter(engine -> engine.id.equals(type)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "blog.database.type must be sqlite, mysql or postgresql"));
    }

    void validateUrl(String url) {
        if (!url.startsWith("jdbc:" + id + ":") || url.length() == ("jdbc:" + id + ":").length()) {
            throw new IllegalArgumentException("blog.database.url must be a JDBC URL for " + id);
        }
    }
}
