package com.qexcel.service;

import com.qexcel.db.GenericQueryRunner;
import com.qexcel.model.ParamDef;
import com.qexcel.model.ParamType;
import com.qexcel.model.QueryDef;
import com.qexcel.model.QueryResult;
import com.qexcel.util.SqlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 쿼리 실행 전체 흐름: 입력값 바인딩 -> 실행 -> 엑셀/SQL 저장 -> 실행이력 기록.
 */
public class QueryExecuteService {

    private static final Logger log = LoggerFactory.getLogger(QueryExecuteService.class);

    private final GenericQueryRunner runner;
    private final ExcelExportService excelExportService;
    private final OutputPathService outputPathService;
    private final RunHistoryService runHistoryService;

    public QueryExecuteService(GenericQueryRunner runner,
                               ExcelExportService excelExportService,
                               OutputPathService outputPathService,
                               RunHistoryService runHistoryService) {
        this.runner = runner;
        this.excelExportService = excelExportService;
        this.outputPathService = outputPathService;
        this.runHistoryService = runHistoryService;
    }

    /**
     * @param def    실행할 쿼리 정의
     * @param inputs '?' 순서대로의 사용자 입력값. DATE 는 LocalDate, NUMBER/TEXT 는 String 권장.
     * @return 실행 결과(엑셀/SQL 파일 경로 포함)
     */
    public ExecutionOutcome execute(QueryDef def, List<Object> inputs) throws Exception {
        SqlValidator.validateSelectOnly(def.getSql());

        int placeholders = SqlValidator.countPlaceholders(def.getSql());
        if (placeholders != def.getParams().size()) {
            throw new IllegalArgumentException(
                    "'?' 개수(%d)와 파라미터 정의(%d)가 일치하지 않습니다."
                            .formatted(placeholders, def.getParams().size()));
        }
        if (inputs.size() != def.getParams().size()) {
            throw new IllegalArgumentException("입력값 개수가 파라미터 정의와 다릅니다.");
        }

        List<Object> bound = new ArrayList<>(inputs.size());
        List<String> display = new ArrayList<>(inputs.size());
        for (int i = 0; i < def.getParams().size(); i++) {
            ParamDef p = def.getParams().get(i);
            Object converted = convert(p, inputs.get(i));
            bound.add(converted);
            display.add(String.valueOf(converted));
        }

        LocalDateTime now = LocalDateTime.now();
        QueryResult result = runner.execute(def.getSql(), bound);

        File excel = excelExportService.export(result, outputPathService.excelFile(def.getQueryName(), now));
        File sqlTxt = outputPathService.writeSqlText(def.getQueryName(), now, buildSqlText(def, display));

        runHistoryService.markRun(def.getQueryName(), now.toLocalDate());
        log.info("쿼리 '{}' 실행 완료 -> {}, {}", def.getQueryName(), excel.getName(), sqlTxt.getName());

        return new ExecutionOutcome(result.getRowCount(), excel, sqlTxt);
    }

    /** ParamDef 형식에 맞춰 입력값을 JDBC 바인딩 가능한 값으로 변환 */
    private Object convert(ParamDef p, Object raw) {
        if (raw == null) {
            return null;
        }
        return switch (p.getType()) {
            case TEXT -> raw.toString();
            case NUMBER -> new BigDecimal(raw.toString().trim());
            case DATE -> toDateString(p, raw);
        };
    }

    private String toDateString(ParamDef p, Object raw) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p.getDateFormat().getPattern());
        if (raw instanceof LocalDate d) {
            return d.format(fmt);
        }
        // 이미 문자열로 들어온 경우 포맷 검증 후 그대로 사용
        String s = raw.toString().trim();
        LocalDate.parse(s, fmt); // 형식 불일치 시 예외
        return s;
    }

    /** txt 로 저장할, 값이 채워진 가독용 SQL 문자열 생성 */
    private String buildSqlText(QueryDef def, List<String> values) {
        StringBuilder sb = new StringBuilder(def.getSql());
        for (String v : values) {
            int idx = sb.indexOf("?");
            if (idx < 0) {
                break;
            }
            String literal = isNumeric(v) ? v : "'" + v + "'";
            sb.replace(idx, idx + 1, literal);
        }
        return sb.toString();
    }

    private boolean isNumeric(String s) {
        try {
            new BigDecimal(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public record ExecutionOutcome(int rowCount, File excelFile, File sqlFile) {
    }
}
