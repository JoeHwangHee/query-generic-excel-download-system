package com.qexcel.service;

import com.qexcel.util.FileNameUtil;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 결과물 저장 경로 관리 및 실행 SQL 텍스트 저장.
 * C:\Temp\query-excel-download\ 하위에 자동 생성한다.
 */
public class OutputPathService {

    public static final Path BASE_DIR = Path.of("C:", "Temp", "query-excel-download");

    public OutputPathService() {
        ensureDir(BASE_DIR);
    }

    /** 엑셀 파일 대상 경로 (쿼리명_실행일시.xlsx) */
    public File excelFile(String queryName, LocalDateTime when) {
        return BASE_DIR.resolve(FileNameUtil.baseName(queryName, when) + ".xlsx").toFile();
    }

    /** 실행된 SQL을 동일 파일명(.txt)으로 저장 */
    public File writeSqlText(String queryName, LocalDateTime when, String resolvedSql) {
        File txt = BASE_DIR.resolve(FileNameUtil.baseName(queryName, when) + ".txt").toFile();
        try {
            Files.writeString(txt.toPath(), resolvedSql, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("SQL 텍스트 저장 실패: " + txt, e);
        }
        return txt;
    }

    private static void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("출력 폴더 생성 실패: " + dir, e);
        }
    }
}
