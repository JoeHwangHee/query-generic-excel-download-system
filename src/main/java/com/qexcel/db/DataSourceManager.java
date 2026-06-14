package com.qexcel.db;

import com.qexcel.model.DbConnectionDef;
import com.qexcel.service.DbStoreService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 등록된 DB 설정명 별로 HikariCP 커넥션 풀을 lazy 하게 생성/캐시한다.
 *
 * <p>쿼리마다 접속 DB 가 다를 수 있으므로, 기존의 단일 DataSource 싱글턴을 대체한다.
 * 풀 설정값은 폐쇄망 읽기전용 조회 용도에 맞춰 상수로 고정한다.</p>
 */
public class DataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);

    private static final int MAX_POOL_SIZE = 5;
    private static final int MIN_IDLE = 1;
    private static final int CONNECTION_TIMEOUT_MS = 10_000;
    private static final boolean READ_ONLY = true;

    private final DbStoreService dbStore;
    private final ConcurrentHashMap<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    public DataSourceManager(DbStoreService dbStore) {
        this.dbStore = dbStore;
    }

    /**
     * 설정명에 해당하는 DataSource 를 돌려준다(없으면 생성/캐시).
     *
     * @throws IllegalArgumentException 등록되지 않은 DB 설정명인 경우
     */
    public DataSource get(String dbName) {
        DbConnectionDef def = dbStore.find(dbName);
        if (def == null) {
            throw new IllegalArgumentException("등록되지 않은 DB 설정입니다: " + dbName);
        }
        return pools.computeIfAbsent(dbName, k -> build(def));
    }

    private HikariDataSource build(DbConnectionDef def) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(def.getJdbcUrl());
        cfg.setUsername(def.getUsername());
        cfg.setPassword(def.getPassword());
        cfg.setDriverClassName(def.getDriver());
        cfg.setMaximumPoolSize(MAX_POOL_SIZE);
        cfg.setMinimumIdle(MIN_IDLE);
        cfg.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        cfg.setReadOnly(READ_ONLY);
        cfg.setPoolName("pool-" + def.getName());

        log.info("DataSource 초기화: {} -> {}", def.getName(), def.getJdbcUrl());
        return new HikariDataSource(cfg);
    }

    /**
     * 풀을 만들지 않고 즉시 접속을 시도한다(연결 테스트 버튼용).
     *
     * @throws SQLException 드라이버 로드 실패 또는 접속 실패 시
     */
    public void testConnection(DbConnectionDef def) throws SQLException {
        try {
            Class.forName(def.getDriver());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC 드라이버를 찾을 수 없습니다: " + def.getDriver(), e);
        }
        int prev = DriverManager.getLoginTimeout();
        DriverManager.setLoginTimeout(CONNECTION_TIMEOUT_MS / 1000);
        try (Connection ignored = DriverManager.getConnection(
                def.getJdbcUrl(), def.getUsername(), def.getPassword())) {
            // 접속 성공 — 별도 처리 없음
        } finally {
            DriverManager.setLoginTimeout(prev);
        }
    }

    /** DB 정의가 변경되면 캐시된 풀을 닫고 제거한다(다음 get 시 재생성). */
    public void invalidate(String dbName) {
        HikariDataSource ds = pools.remove(dbName);
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    public void shutdownAll() {
        for (HikariDataSource ds : pools.values()) {
            if (!ds.isClosed()) {
                ds.close();
            }
        }
        pools.clear();
    }
}
