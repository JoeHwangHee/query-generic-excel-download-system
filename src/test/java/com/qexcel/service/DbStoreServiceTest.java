package com.qexcel.service;

import com.qexcel.model.DbConnectionDef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbStoreServiceTest {

    @TempDir
    Path tmp;

    private DbConnectionDef def(String name, String url) {
        DbConnectionDef d = new DbConnectionDef();
        d.setName(name);
        d.setJdbcUrl(url);
        d.setUsername("user");
        d.setPassword("pw");
        return d;
    }

    @Test
    void savesAndReloadsFromFile() {
        Path file = tmp.resolve("databases.md");
        DbStoreService store = new DbStoreService(file);
        store.save(def("운영", "jdbc:mysql://prod/db"));
        store.save(def("개발", "jdbc:mysql://dev/db"));

        // 새 인스턴스로 파일에서 다시 읽어 라운드트립 검증
        DbStoreService reloaded = new DbStoreService(file);
        reloaded.loadIntoMemory();

        List<DbConnectionDef> all = reloaded.findAll();
        assertEquals(2, all.size());
        DbConnectionDef prod = reloaded.find("운영");
        assertEquals("jdbc:mysql://prod/db", prod.getJdbcUrl());
        assertEquals("user", prod.getUsername());
        assertEquals(DbConnectionDef.DEFAULT_DRIVER, prod.getDriver());
    }

    @Test
    void deleteRemovesFromFile() {
        Path file = tmp.resolve("databases.md");
        DbStoreService store = new DbStoreService(file);
        store.save(def("운영", "jdbc:mysql://prod/db"));
        store.delete("운영");

        DbStoreService reloaded = new DbStoreService(file);
        reloaded.loadIntoMemory();
        assertTrue(reloaded.findAll().isEmpty());
        assertNull(reloaded.find("운영"));
    }
}
