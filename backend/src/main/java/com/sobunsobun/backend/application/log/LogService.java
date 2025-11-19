package com.sobunsobun.backend.application.log;

import com.sobunsobun.backend.dto.log.LogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 로그 파일 조회 서비스
 *
 * 담당 기능:
 * - 로그 파일 목록 조회
 * - 로그 파일 내용 조회 (최근 N줄)
 * - 에러 로그 조회
 */
@Slf4j
@Service
public class LogService {

    @Value("${logging.file.path:logs}")
    private String logPath;

    @Value("${logging.file.name:sobunsobun}")
    private String logFileName;

    /**
     * 로그 파일 내용 조회 (최근 N줄)
     *
     * @param logType 로그 타입 (all, error, sql)
     * @param lines 조회할 라인 수 (기본 100줄)
     * @return 로그 응답
     */
    public LogResponse getLogs(String logType, int lines) {
        log.info("[관리자 작동] 로그 조회 - 타입: {}, 라인: {}", logType, lines);

        String fileName = getLogFileName(logType);
        Path filePath = Paths.get(logPath, fileName);

        if (!Files.exists(filePath)) {
            log.warn("로그 파일이 존재하지 않음 - 파일: {}", filePath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "로그 파일을 찾을 수 없습니다");
        }

        try {
            List<String> allLines = Files.readAllLines(filePath);
            long totalLines = allLines.size();

            // 최근 N줄만 추출
            int startIndex = Math.max(0, allLines.size() - lines);
            List<String> recentLogs = allLines.subList(startIndex, allLines.size());

            log.info("로그 조회 완료 - 전체: {}줄, 반환: {}줄", totalLines, recentLogs.size());

            return LogResponse.builder()
                    .logType(logType)
                    .fileName(fileName)
                    .totalLines(totalLines)
                    .requestedLines(lines)
                    .logs(recentLogs)
                    .build();

        } catch (IOException e) {
            log.error("로그 파일 읽기 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로그 파일을 읽을 수 없습니다");
        }
    }

    /**
     * 사용 가능한 로그 파일 목록 조회
     *
     * @return 로그 파일 목록
     */
    public List<String> getLogFileList() {
        log.info("[관리자 작동] 로그 파일 목록 조회");

        Path logDirectory = Paths.get(logPath);

        if (!Files.exists(logDirectory)) {
            log.warn("로그 디렉토리가 존재하지 않음 - 경로: {}", logDirectory);
            return Collections.emptyList();
        }

        try {
            return Files.list(logDirectory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("로그 파일 목록 조회 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로그 파일 목록을 조회할 수 없습니다");
        }
    }

    /**
     * 특정 날짜의 로그 파일 조회
     *
     * @param logType 로그 타입
     * @param date 날짜 (yyyy-MM-dd)
     * @param lines 조회할 라인 수
     * @return 로그 응답
     */
    public LogResponse getLogsByDate(String logType, String date, int lines) {
        log.info("[관리자 작동] 날짜별 로그 조회 - 타입: {}, 날짜: {}, 라인: {}", logType, date, lines);

        String fileName = getLogFileNameByDate(logType, date);
        Path filePath = Paths.get(logPath, fileName);

        if (!Files.exists(filePath)) {
            log.warn("날짜별 로그 파일이 존재하지 않음 - 파일: {}", filePath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 날짜의 로그 파일을 찾을 수 없습니다");
        }

        try {
            List<String> allLines = Files.readAllLines(filePath);
            long totalLines = allLines.size();

            // 최근 N줄만 추출
            int startIndex = Math.max(0, allLines.size() - lines);
            List<String> recentLogs = allLines.subList(startIndex, allLines.size());

            return LogResponse.builder()
                    .logType(logType)
                    .fileName(fileName)
                    .totalLines(totalLines)
                    .requestedLines(lines)
                    .logs(recentLogs)
                    .build();

        } catch (IOException e) {
            log.error("날짜별 로그 파일 읽기 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로그 파일을 읽을 수 없습니다");
        }
    }

    /**
     * 로그 타입에 따른 파일명 반환
     */
    private String getLogFileName(String logType) {
        return switch (logType.toLowerCase()) {
            case "error" -> logFileName + "-error.log";
            case "sql" -> logFileName + "-sql.log";
            default -> logFileName + ".log";
        };
    }

    /**
     * 날짜별 로그 파일명 반환
     */
    private String getLogFileNameByDate(String logType, String date) {
        String baseFileName = switch (logType.toLowerCase()) {
            case "error" -> logFileName + "-error";
            case "sql" -> logFileName + "-sql";
            default -> logFileName;
        };
        return baseFileName + "-" + date + "-1.log";
    }
}

