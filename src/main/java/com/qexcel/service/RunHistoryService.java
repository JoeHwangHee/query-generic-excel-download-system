package com.qexcel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qexcel.model.RunHistory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 배치 중복 실행 방지를 위한 실행이력(run-history.json) 영속화.
 */
public class RunHistoryService {

    private final Path file;
    private final ObjectMapper mapper;
    private RunHistory history;

    public RunHistoryService(Path file) {
        this.file = file;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            history = new RunHistory();
            return;
        }
        try {
            history = mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), RunHistory.class);
        } catch (IOException e) {
            throw new UncheckedIOException("실행이력 로드 실패: " + file, e);
        }
    }

    public LocalDate getLastRun(String queryName) {
        return history.getLastRun(queryName);
    }

    public synchronized void markRun(String queryName, LocalDate date) {
        history.markRun(queryName, date);
        persist();
    }

    private void persist() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, mapper.writeValueAsString(history), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("실행이력 저장 실패: " + file, e);
        }
    }
}
