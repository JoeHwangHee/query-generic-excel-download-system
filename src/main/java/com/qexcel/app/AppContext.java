package com.qexcel.app;

import com.qexcel.db.DataSourceManager;
import com.qexcel.db.GenericQueryRunner;
import com.qexcel.service.DbStoreService;
import com.qexcel.service.ExcelExportService;
import com.qexcel.service.OutputPathService;
import com.qexcel.service.QueryExecuteService;
import com.qexcel.service.QueryStoreService;
import com.qexcel.service.RunHistoryService;
import com.qexcel.service.ScheduleService;

import java.nio.file.Path;

/**
 * 애플리케이션 의존성 조립(수동 DI 컨테이너).
 */
public class AppContext {

    // 쿼리 정의 / DB 정의 / 실행이력 저장 위치
    private static final Path QUERIES_MD = Path.of("docs", "queries.md");
    private static final Path DATABASES_MD = Path.of("docs", "databases.md");
    private static final Path RUN_HISTORY = OutputPathService.BASE_DIR.resolve("run-history.json");

    private final QueryStoreService queryStoreService;
    private final DbStoreService dbStoreService;
    private final DataSourceManager dataSourceManager;
    private final RunHistoryService runHistoryService;
    private final ScheduleService scheduleService;
    private final QueryExecuteService queryExecuteService;

    public AppContext() {
        this.queryStoreService = new QueryStoreService(QUERIES_MD);
        this.dbStoreService = new DbStoreService(DATABASES_MD);
        this.dataSourceManager = new DataSourceManager(dbStoreService);
        this.runHistoryService = new RunHistoryService(RUN_HISTORY);
        this.scheduleService = new ScheduleService();

        GenericQueryRunner runner = new GenericQueryRunner();
        this.queryExecuteService = new QueryExecuteService(
                runner,
                dataSourceManager,
                dbStoreService,
                new ExcelExportService(),
                new OutputPathService(),
                runHistoryService);
    }

    public void start() {
        queryStoreService.loadIntoMemory();
        dbStoreService.loadIntoMemory();
    }

    public QueryStoreService queryStore() {
        return queryStoreService;
    }

    public DbStoreService dbStore() {
        return dbStoreService;
    }

    public DataSourceManager dataSources() {
        return dataSourceManager;
    }

    public RunHistoryService runHistory() {
        return runHistoryService;
    }

    public ScheduleService schedule() {
        return scheduleService;
    }

    public QueryExecuteService queryExecute() {
        return queryExecuteService;
    }
}
