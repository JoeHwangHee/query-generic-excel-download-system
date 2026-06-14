package com.qexcel.db;

import com.qexcel.model.DbConnectionDef;
import com.qexcel.service.DbStoreService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceManagerTest {

    @TempDir
    Path tmp;

    private DataSourceManager manager;

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.shutdownAll();
        }
    }

    private DbConnectionDef h2(String name) {
        DbConnectionDef d = new DbConnectionDef();
        d.setName(name);
        // 인메모리 H2 (JVM 동안 유지)
        d.setJdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        d.setUsername("sa");
        d.setPassword("");
        d.setDriver("org.h2.Driver");
        return d;
    }

    private DbStoreService storeWith(DbConnectionDef... defs) {
        DbStoreService store = new DbStoreService(tmp.resolve("databases.md"));
        for (DbConnectionDef d : defs) {
            store.save(d);
        }
        return store;
    }

    @Test
    void getReturnsUsableDataSourceForRegisteredDb() throws Exception {
        manager = new DataSourceManager(storeWith(h2("alpha")));
        DataSource ds = manager.get("alpha");
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void getCachesSameDataSourcePerName() {
        manager = new DataSourceManager(storeWith(h2("alpha")));
        assertEquals(manager.get("alpha"), manager.get("alpha"));
    }

    @Test
    void getRejectsUnregisteredDb() {
        manager = new DataSourceManager(storeWith(h2("alpha")));
        assertThrows(IllegalArgumentException.class, () -> manager.get("없는DB"));
    }

    @Test
    void testConnectionSucceedsForValidDb() {
        manager = new DataSourceManager(storeWith());
        // 예외가 나지 않으면 성공
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> manager.testConnection(h2("probe")));
    }

    @Test
    void testConnectionFailsForBadDriver() {
        manager = new DataSourceManager(storeWith());
        DbConnectionDef bad = h2("bad");
        bad.setDriver("no.such.Driver");
        assertThrows(java.sql.SQLException.class, () -> manager.testConnection(bad));
    }
}
