package com.qexcel.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Swing 진입점이라 main() 실행은 테스트하지 않는다. 클래스가 정상 로드되는지만 확인한다.
 */
class MainTest {

    @Test
    void classLoads() {
        assertNotNull(Main.class);
    }
}
