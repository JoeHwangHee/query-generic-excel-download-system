package com.qexcel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qexcel.model.QueryDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 쿼리 정의 저장소.
 *
 * <p>별도 DB 테이블 없이 프로젝트 내부 폴더의 md 파일(JSON 코드펜스)에 저장한다.
 * 기동 시 전체를 읽어 로컬 메모리(캐시)에 올리고, 런타임 동안만 조회/수정한다.</p>
 */
public class QueryStoreService {

    private static final Logger log = LoggerFactory.getLogger(QueryStoreService.class);

    // ```json ... ``` 코드펜스 추출
    private static final Pattern JSON_BLOCK =
            Pattern.compile("```json\\s*(.*?)```", Pattern.DOTALL);

    private final Path mdFile;
    private final ObjectMapper mapper;
    private final Map<String, QueryDef> cache = new LinkedHashMap<>();

    public QueryStoreService(Path mdFile) {
        this.mdFile = mdFile;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** 기동 시 호출: md 파일을 읽어 캐시에 적재 */
    public synchronized void loadIntoMemory() {
        cache.clear();
        if (!Files.exists(mdFile)) {
            log.info("쿼리 저장 파일 없음, 신규 시작: {}", mdFile);
            return;
        }
        try {
            String content = Files.readString(mdFile, StandardCharsets.UTF_8);
            Matcher m = JSON_BLOCK.matcher(content);
            if (m.find()) {
                String json = m.group(1).trim();
                if (!json.isEmpty()) {
                    QueryDef[] defs = mapper.readValue(json, QueryDef[].class);
                    for (QueryDef d : defs) {
                        cache.put(d.getQueryName(), d);
                    }
                }
            }
            log.info("쿼리 {}건 메모리 적재", cache.size());
        } catch (IOException e) {
            throw new UncheckedIOException("쿼리 파일 로드 실패: " + mdFile, e);
        }
    }

    public synchronized List<QueryDef> findAll() {
        return new ArrayList<>(cache.values());
    }

    public synchronized QueryDef find(String queryName) {
        return cache.get(queryName);
    }

    /** 저장(신규/수정) 후 파일 동기화 */
    public synchronized void save(QueryDef def) {
        cache.put(def.getQueryName(), def);
        persist();
    }

    public synchronized void delete(String queryName) {
        cache.remove(queryName);
        persist();
    }

    private void persist() {
        try {
            Files.createDirectories(mdFile.getParent());
            String json = mapper.writeValueAsString(new ArrayList<>(cache.values()));
            String md = """
                    # 저장된 쿼리 정의

                    이 파일은 애플리케이션이 관리합니다. 아래 JSON 블록을 직접 수정하지 마세요.

                    ```json
                    %s
                    ```
                    """.formatted(json);
            Files.writeString(mdFile, md, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("쿼리 파일 저장 실패: " + mdFile, e);
        }
    }
}
