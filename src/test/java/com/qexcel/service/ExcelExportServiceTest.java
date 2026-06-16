package com.qexcel.service;

import com.qexcel.model.DateFormatType;
import com.qexcel.model.QueryResult;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelExportServiceTest {

    private final ExcelExportService service = new ExcelExportService();

    @Test
    void writesMultipleSheetsWithGivenNames(@TempDir Path dir) throws Exception {
        QueryResult r1 = intResult(1);
        QueryResult r2 = intResult(2);
        File out = dir.resolve("multi.xlsx").toFile();

        service.exportSheets(List.of(r1, r2), List.of("월간_1", "월간_2"),
                DateFormatType.YYYY_MM_DD, out);

        try (Workbook wb = WorkbookFactory.create(out)) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("월간_1", wb.getSheetName(0));
            assertEquals("월간_2", wb.getSheetName(1));
        }
    }

    @Test
    void formatsDateColumnAsConfiguredPattern(@TempDir Path dir) throws Exception {
        QueryResult r = dateResult(Types.DATE, Date.valueOf("2026-06-16"));

        File hyphen = dir.resolve("hyphen.xlsx").toFile();
        service.exportSheets(List.of(r), List.of("s"), DateFormatType.YYYY_MM_DD, hyphen);
        assertEquals("2026-06-16", firstDataCell(hyphen));

        File compact = dir.resolve("compact.xlsx").toFile();
        service.exportSheets(List.of(r), List.of("s"), DateFormatType.YYYYMMDD, compact);
        assertEquals("20260616", firstDataCell(compact));
    }

    @Test
    void formatsTimestampKeepingTime(@TempDir Path dir) throws Exception {
        QueryResult r = dateResult(Types.TIMESTAMP, Timestamp.valueOf("2026-06-16 09:30:00"));
        File out = dir.resolve("ts.xlsx").toFile();
        service.exportSheets(List.of(r), List.of("s"), DateFormatType.YYYY_MM_DD, out);
        assertEquals("2026-06-16 09:30:00", firstDataCell(out));
    }

    private static QueryResult intResult(int idVal) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("id", idVal);
        return new QueryResult(List.of("id"), List.of(Types.INTEGER), List.of(row));
    }

    private static QueryResult dateResult(int sqlType, Object value) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("d", value);
        return new QueryResult(List.of("d"), List.of(sqlType), List.of(row));
    }

    /** 헤더(0행) 다음 첫 데이터 셀의 문자열 값 */
    private static String firstDataCell(File xlsx) throws Exception {
        try (Workbook wb = WorkbookFactory.create(xlsx)) {
            Sheet sheet = wb.getSheetAt(0);
            return sheet.getRow(1).getCell(0).getStringCellValue();
        }
    }
}
