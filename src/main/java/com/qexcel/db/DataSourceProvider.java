package com.qexcel.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * db.properties 를 읽어 HikariCP DataSource 를 생성/보관한다.
 * 폐쇄망 내부 MySQL/MariaDB 접속용. 읽기 전용(readOnly) 풀로 운용.
 */
public final class DataSourceProvider {

    private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);
    private static volatile HikariDataSource dataSource;

    private DataSourceProvider() {
    }

    public static synchronized DataSource get() {
        if (dataSource == null) {
            dataSource = build();
        }
        return dataSource;
    }

    private static HikariDataSource build() {
        Properties props = new Properties();
        try (InputStream in = DataSourceProvider.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties 를 찾을 수 없습니다.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("db.properties 로드 실패", e);
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("jdbc.url"));
        cfg.setUsername(props.getProperty("jdbc.username"));
        cfg.setPassword(props.getProperty("jdbc.password"));
        cfg.setDriverClassName(props.getProperty("jdbc.driver"));
        cfg.setMaximumPoolSize(intProp(props, "pool.maximumPoolSize", 5));
        cfg.setMinimumIdle(intProp(props, "pool.minimumIdle", 1));
        cfg.setConnectionTimeout(intProp(props, "pool.connectionTimeoutMs", 10000));
        cfg.setReadOnly(Boolean.parseBoolean(props.getProperty("pool.readOnly", "true")));
        cfg.setPoolName("query-excel-pool");

        log.info("DataSource 초기화: {}", cfg.getJdbcUrl());
        return new HikariDataSource(cfg);
    }

    private static int intProp(Properties p, String key, int def) {
        String v = p.getProperty(key);
        return v == null ? def : Integer.parseInt(v.trim());
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
