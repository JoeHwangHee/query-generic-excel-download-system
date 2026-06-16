package com.qexcel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.qexcel.model.DateFormatType;
import com.qexcel.model.QueryResult;
import com.qexcel.util.FileNameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 쿼리 결과를 EasyExcel Dynamic Head 방식으로 .xlsx 파일에 내보낸다.
 * 컬럼명을 헤더(첫 행)로, 이후 행을 순서대로 기록한다.
 *
 * <p>여러 SELECT 결과는 한 파일의 서로 다른 시트로 저장한다. DATE/DATETIME 컬럼은
 * 쿼리에 지정된 {@link DateFormatType} 으로 문자열 변환해 출력한다(엑셀 셀은 텍스트가 된다).</p>
 *
 * <p>참고: 진정한 대용량 스트리밍이 필요하면 ResultSet 을 한 행씩
 * ExcelWriter 로 흘려보내는 방식으로 확장할 수 있다(추후 개선 항목).</p>
 */
public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);

    /** 엑셀 시트명 최대 길이 */
    private static final int MAX_SHEET_NAME = 31;

    /** 단일 결과를 기본 시트("result")로 내보낸다(하위호환). */
    public File export(QueryResult result, File targetXlsx) {
        return exportSheets(List.of(result), List.of("result"), DateFormatType.YYYY_MM_DD, targetXlsx);
    }

    /**
     * 여러 결과를 시트별로 한 파일에 내보낸다.
     *
     * @param results          시트로 저장할 SELECT 결과들(순서 보존)
     * @param sheetNames       각 결과의 시트명({@link #sheetNames} 로 생성). results 와 크기 동일
     * @param outputDateFormat DATE/DATETIME 컬럼에 적용할 출력 포맷
     */
    public File exportSheets(List<QueryResult> results, List<String> sheetNames,
                             DateFormatType outputDateFormat, File targetXlsx) {
        try (ExcelWriter writer = EasyExcel.write(targetXlsx).build()) {
            long totalRows = 0;
            for (int i = 0; i < results.size(); i++) {
                QueryResult r = results.get(i);
                WriteSheet sheet = EasyExcel.writerSheet(i, sheetNames.get(i)).head(headOf(r)).build();
                writer.write(dataOf(r, outputDateFormat), sheet);
                totalRows += r.getRowCount();
            }
            log.info("엑셀 생성: {} ({}시트, 총 {}행)",
                    targetXlsx.getAbsolutePath(), results.size(), totalRows);
        }
        return targetXlsx;
    }

    /**
     * 쿼리명 기반 시트명을 만든다. {@code 쿼리명_1, 쿼리명_2 ...}
     * 엑셀 금지문자 정리, 31자 제한, 중복 보정을 적용한다.
     */
    public static List<String> sheetNames(String queryName, int count) {
        List<String> names = new ArrayList<>(count);
        Set<String> used = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            // FileNameUtil.sanitize 가 \ / : * ? " < > | 제거. 엑셀이 추가로 막는 [ ] 도 정리.
            String base = (FileNameUtil.sanitize(queryName) + "_" + (i + 1)).replaceAll("[\\[\\]]", "_");
            names.add(uniqueWithin(base, used));
        }
        return names;
    }

    private static String uniqueWithin(String base, Set<String> used) {
        String candidate = truncate(base);
        int dup = 2;
        while (used.contains(candidate)) {
            String suffix = "_" + dup++;
            int head = Math.max(0, MAX_SHEET_NAME - suffix.length());
            candidate = truncate(base.length() > head ? base.substring(0, head) : base) + suffix;
        }
        used.add(candidate);
        return candidate;
    }

    private static String truncate(String s) {
        return s.length() > MAX_SHEET_NAME ? s.substring(0, MAX_SHEET_NAME) : s;
    }

    private List<List<String>> headOf(QueryResult result) {
        List<List<String>> head = new ArrayList<>(result.getColumns().size());
        for (String col : result.getColumns()) {
            List<String> h = new ArrayList<>(1);
            h.add(col);
            head.add(h);
        }
        return head;
    }

    private List<List<Object>> dataOf(QueryResult result, DateFormatType outputDateFormat) {
        List<String> cols = result.getColumns();
        List<Integer> types = result.getColumnTypes();
        List<List<Object>> data = new ArrayList<>(result.getRowCount());
        for (LinkedHashMap<String, Object> row : result.getRows()) {
            List<Object> line = new ArrayList<>(cols.size());
            for (int c = 0; c < cols.size(); c++) {
                line.add(formatCell(row.get(cols.get(c)), types.get(c), outputDateFormat));
            }
            data.add(line);
        }
        return data;
    }

    /** DATE/DATETIME 컬럼이면 지정 포맷의 문자열로, 그 외에는 원본 값을 그대로 반환. */
    private Object formatCell(Object value, int sqlType, DateFormatType fmt) {
        if (value == null) {
            return null;
        }
        boolean isDate = sqlType == Types.DATE;
        boolean isTimestamp = sqlType == Types.TIMESTAMP || sqlType == Types.TIMESTAMP_WITH_TIMEZONE;
        if (!isDate && !isTimestamp) {
            return value;
        }
        LocalDateTime ldt = toDateTime(value);
        if (ldt == null) {
            return value; // 시간형이 아니면 원본 유지
        }
        String pattern = isTimestamp ? fmt.getPattern() + " HH:mm:ss" : fmt.getPattern();
        return ldt.format(DateTimeFormatter.ofPattern(pattern));
    }

    private static LocalDateTime toDateTime(Object v) {
        if (v instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate().atStartOfDay();
        }
        if (v instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (v instanceof LocalDate ld) {
            return ld.atStartOfDay();
        }
        if (v instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime();
        }
        return null;
    }
}
