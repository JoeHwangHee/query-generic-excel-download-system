package com.qexcel.service;

import com.qexcel.db.DataSourceManager;
import com.qexcel.db.DbConnectionException;
import com.qexcel.db.GenericQueryRunner;
import com.qexcel.model.QueryDef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecuteServiceTest {

    @TempDir
    Path tmp;

    private QueryExecuteService service() {
        DbStoreService dbStore = new DbStoreService(tmp.resolve("databases.md"));
        DataSourceManager dsManager = new DataSourceManager(dbStore);
        // excel/output/runHistory 는 DB 해석 단계 이전에 도달하지 않으므로 null 로 둔다
        return new QueryExecuteService(new GenericQueryRunner(), dsManager, dbStore, null, null, null);
    }

    private QueryDef def(String dbName) {
        QueryDef d = new QueryDef();
        d.setQueryName("q");
        d.setSql("SELECT 1");
        d.setDbName(dbName);
        return d;
    }

    @Test
    void rejectsQueryWithoutDb() {
        assertThrows(DbConnectionException.class,
                () -> service().execute(def(null), List.of()));
    }

    @Test
    void rejectsUnregisteredDb() {
        assertThrows(IllegalArgumentException.class,
                () -> service().execute(def("없는DB"), List.of()));
    }
}
