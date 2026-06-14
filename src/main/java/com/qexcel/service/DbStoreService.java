package com.qexcel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qexcel.model.DbConnectionDef;
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
 * DB 접속 정의 저장소.
 *
 * <p>{@link QueryStoreService} 와 동일하게 프로젝트 내부 md 파일(JSON 코드펜스)에 저장한다.
 * 기동 시 전체를 읽어 로컬 메모리(캐시)에 올리고, 런타임 동안 조회/수정한다.</p>
 */
public class DbStoreService {

    private static final Logger log = LoggerFactory.getLogger(DbStoreService.class);

    // ```json ... ``` 코드펜스 추출
    private static final Pattern JSON_BLOCK =
            Pattern.compile("```json\\s*(.*?)```", Pattern.DOTALL);

    private final Path mdFile;
    private final ObjectMapper mapper;
    private final Map<String, DbConnectionDef> cache = new LinkedHashMap<>();

    public DbStoreService(Path mdFile) {
        this.mdFile = mdFile;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** 기동 시 호출: md 파일을 읽어 캐시에 적재 */
    public synchronized void loadIntoMemory() {
        cache.clear();
        if (!Files.exists(mdFile)) {
            log.info("DB 저장 파일 없음, 신규 시작: {}", mdFile);
            return;
        }
        try {
            String content = Files.readString(mdFile, StandardCharsets.UTF_8);
            Matcher m = JSON_BLOCK.matcher(content);
            if (m.find()) {
                String json = m.group(1).trim();
                if (!json.isEmpty()) {
                    DbConnectionDef[] defs = mapper.readValue(json, DbConnectionDef[].class);
                    for (DbConnectionDef d : defs) {
                        cache.put(d.getName(), d);
                    }
                }
            }
            log.info("DB {}건 메모리 적재", cache.size());
        } catch (IOException e) {
            throw new UncheckedIOException("DB 파일 로드 실패: " + mdFile, e);
        }
    }

    public synchronized List<DbConnectionDef> findAll() {
        return new ArrayList<>(cache.values());
    }

    public synchronized DbConnectionDef find(String name) {
        return cache.get(name);
    }

    /** 저장(신규/수정) 후 파일 동기화 */
    public synchronized void save(DbConnectionDef def) {
        cache.put(def.getName(), def);
        persist();
    }

    public synchronized void delete(String name) {
        cache.remove(name);
        persist();
    }

    private void persist() {
        try {
            Files.createDirectories(mdFile.getParent());
            String json = mapper.writeValueAsString(new ArrayList<>(cache.values()));
            String md = """
                    # 저장된 DB 접속 정의

                    이 파일은 애플리케이션이 관리합니다. 아래 JSON 블록을 직접 수정하지 마세요.

                    ```json
                    %s
                    ```
                    """.formatted(json);
            Files.writeString(mdFile, md, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("DB 파일 저장 실패: " + mdFile, e);
        }
    }
}
