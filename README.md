# 쿼리 기반 제네릭 엑셀 다운로드 시스템

쿼리별로 결과 형태가 달라 매번 DTO를 만들 수 없는 상황에서, 입력한 쿼리의 결과
형태 그대로 엑셀로 내려받기 위한 Windows 데스크톱 도구.

## 기술 스택
- Java 25, Swing (+ LGoodDatePicker 달력 선택기)
- MySQL/MariaDB (JDBC + HikariCP) — 동적 컬럼 결과는 순수 JDBC로 처리해 컬럼 순서 보존
- Alibaba EasyExcel Dynamic Head 방식으로 `.xlsx` 생성
- 쿼리 정의는 `docs/queries.md`(JSON 블록)에 저장하고 기동 시 메모리로 적재

> 참고: 원 요구사항의 `.xls` + EasyExcel 메모리절약은 기술적으로 충돌하여 `.xlsx`로 확정.

## 빌드 / 실행
```bash
./gradlew shadowJar        # build/libs/query-excel-download.jar (fat jar)
java -jar build/libs/query-excel-download.jar
```
개발/IntelliJ에서는 `com.qexcel.app.Main` 실행. (빌드 도구: Gradle 9.1+ / JDK 25)

### 폐쇄망(오프라인) 빌드
사내 미러에 본 프로젝트 의존성/Gradle 플러그인이 없으므로, 인터넷이 되는 PC에서
캐시를 채운 뒤 폐쇄망으로 이관해 `--offline` 으로 빌드한다.
```bash
# [인터넷 PC] 전용 캐시 디렉터리로 전체 빌드 → Gradle 배포본·플러그인·의존성 모두 캐시됨
GRADLE_USER_HOME=./offline-cache ./gradlew shadowJar

# [전달] offline-cache/ + 프로젝트 소스 + gradlew + gradle/wrapper/* 를 폐쇄망으로 복사
#  - 추가 안정화: gradle/wrapper/gradle-wrapper.properties 의 distributionUrl 을
#    file: 경로나 사내 미러 URL로 바꾸면 wrapper의 인터넷 다운로드 시도가 사라진다.

# [폐쇄망 PC] 캐시를 지정하고 오프라인 빌드 (JDK 25 필요)
GRADLE_USER_HOME=./offline-cache ./gradlew shadowJar --offline
```

## 설정
- DB 접속정보: `docs/databases.md` (앱이 관리, JSON 블록). 쿼리 저장 화면의 **DB 추가**로
  설정명 단위로 등록하며, 등록 시 **연결 테스트** 가능. 기동 시 메모리로 적재
- 결과 저장 경로: `C:\Temp\query-excel-download\`
  - 엑셀: `쿼리명_yyyyMMdd_HHmmss.xlsx`
  - 실행 SQL: `쿼리명_yyyyMMdd_HHmmss.txt`
  - 실행이력(배치 중복방지): `run-history.json`

## 주요 흐름
1. **쿼리 저장** — 쿼리명/SQL(`?` 변수)/배치조건/**실행 DB** 지정, `?`별 형식(TEXT·NUMBER·DATE) 설정
2. **쿼리 실행** — 형식에 맞는 입력칸 동적 생성(DATE는 달력), 쿼리에 지정된 DB로 접속해
   백그라운드 실행 (접속 불가 시 안내 메시지)
3. **다운로드** — 결과 엑셀 + 실행 SQL을 동일 파일명으로 자동 저장
4. **기동 배치** — 오늘(또는 미실행 익일)이 배치일이면 알림 후 하나씩 실행창 표시

## 패키지 구조
```
app/      Main, AppContext (수동 DI)
ui/       MainFrame, QuerySaveDialog, QueryRunDialog, DbConnectionDialog, BatchAlertFlow
model/    QueryDef, ParamDef, DbConnectionDef, QueryResult, RunHistory, enum(ParamType/DateFormatType/ScheduleType)
service/  QueryStore/DbStore/QueryExecute/ExcelExport/Schedule/RunHistory/OutputPath
db/       DataSourceManager(이름별 Hikari 풀), GenericQueryRunner(JDBC), DbConnectionException
util/     SqlValidator(SELECT-only, ? 카운트), FileNameUtil
```

## 배치조건 정의 (해석)
원 요구사항이 주/월 단위 혼재로 모호하여 아래로 해석함. 의미 확정 시
`ScheduleService` 의 계산 로직만 수정하면 됨.

| enum | 의미 |
|---|---|
| `NONE` | 없음 |
| `EVERY_MONDAY` | 매주 월요일 |
| `FIRST_MONDAY` | 매월 첫째 주 월요일 |
| `DAY_1` | 매월 1일 |
| `DAY_26` | 매월 26일 |
| `LAST_WEEK_MONDAY` | 매월 마지막 주 월요일 |

## 남은 작업 / 확장 포인트
- 배치조건 의미 확정 및 주말/공휴일 스킵 규칙 보강
- 대용량 시 ResultSet → ExcelWriter 행 단위 스트리밍(진정한 메모리 절약)
- 폐쇄망 배포: 의존성 오프라인 미러 + fat jar 또는 jpackage(exe)
- 빌드 환경 Java 25 (현재 코드는 Java 21 문법 호환)
