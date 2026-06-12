package com.qexcel.service;

import com.alibaba.excel.EasyExcel;
import com.qexcel.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 쿼리 결과를 EasyExcel Dynamic Head 방식으로 .xlsx 파일에 내보낸다.
 * 컬럼명을 헤더(첫 행)로, 이후 행을 순서대로 기록한다.
 *
 * <p>참고: 진정한 대용량 스트리밍이 필요하면 ResultSet 을 한 행씩
 * ExcelWriter 로 흘려보내는 방식으로 확장할 수 있다(추후 개선 항목).</p>
 */
public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);

    /** @return 생성된 엑셀 파일 */
    public File export(QueryResult result, File targetXlsx) {
        // Dynamic Head: List<List<String>> 형태의 동적 헤더
        List<List<String>> head = new ArrayList<>(result.getColumns().size());
        for (String col : result.getColumns()) {
            List<String> h = new ArrayList<>(1);
            h.add(col);
            head.add(h);
        }

        // 데이터: 컬럼 순서대로 값만 추출
        List<List<Object>> data = new ArrayList<>(result.getRowCount());
        for (LinkedHashMap<String, Object> row : result.getRows()) {
            List<Object> line = new ArrayList<>(result.getColumns().size());
            for (String col : result.getColumns()) {
                line.add(row.get(col));
            }
            data.add(line);
        }

        EasyExcel.write(targetXlsx)
                .head(head)
                .sheet("result")
                .doWrite(data);

        log.info("엑셀 생성: {} ({}행)", targetXlsx.getAbsolutePath(), result.getRowCount());
        return targetXlsx;
    }
}
