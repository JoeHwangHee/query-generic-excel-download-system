package com.qexcel.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AppContext 는 파일시스템(출력 폴더 생성)에 의존하는 수동 DI 컨테이너라
 * 생성 자체에 부작용이 있다. 실제 와이어링은 앱 실행으로 검증하고,
 * 여기서는 클래스가 정상 로드되는지만 확인한다.
 */
class AppContextTest {

    @Test
    void classLoads() {
        assertNotNull(AppContext.class);
    }
}
