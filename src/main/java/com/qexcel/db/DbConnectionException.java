package com.qexcel.db;

/**
 * 쿼리 실행 대상 DB 에 접속할 수 없을 때 던진다.
 *
 * <p>메시지에는 어떤 DB 설정에 왜 접속하지 못했는지 사용자 안내 문구를 담는다.
 * UI 는 이 메시지를 그대로 사용자에게 보여준다.</p>
 */
public class DbConnectionException extends RuntimeException {

    public DbConnectionException(String message) {
        super(message);
    }

    public DbConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
