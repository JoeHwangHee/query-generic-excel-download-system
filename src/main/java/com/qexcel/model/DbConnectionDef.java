package com.qexcel.model;

/**
 * DB 접속 한 건의 정의. databases.md 안에 JSON 형태로 직렬화된다.
 *
 * <p>{@code name} 이 식별자이며 {@link QueryDef#getDbName()} 가 이 값을 참조한다.
 * 풀 설정(최대 커넥션 수, 타임아웃 등)은 {@code DataSourceManager} 의 상수 기본값을 쓴다.</p>
 */
public class DbConnectionDef {

    public static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";

    /** 설정명 (식별자 / 콤보박스 표시) */
    private String name;
    private String jdbcUrl;
    private String username;
    private String password;
    /** JDBC 드라이버 클래스명 */
    private String driver = DEFAULT_DRIVER;

    public DbConnectionDef() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
}
