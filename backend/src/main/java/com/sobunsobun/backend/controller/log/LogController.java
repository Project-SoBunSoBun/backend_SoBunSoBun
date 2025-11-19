package com.sobunsobun.backend.controller.log;

import com.sobunsobun.backend.application.log.LogService;
import com.sobunsobun.backend.dto.log.LogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 로그 조회 API 컨트롤러
 *
 * 관리자 전용 API로, 서버 로그를 조회할 수 있습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Logs", description = "관리자 로그 조회 API")
public class LogController {

    private final LogService logService;

    /**
     * 최근 로그 조회
     *
     * @param logType 로그 타입 (all, error, sql) - 기본값: all
     * @param lines 조회할 라인 수 - 기본값: 100
     * @return 로그 응답
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "최근 로그 조회", description = "최근 N줄의 로그를 조회합니다 (관리자 전용)")
    public ResponseEntity<LogResponse> getLogs(
            @Parameter(description = "로그 타입 (all, error, sql)")
            @RequestParam(defaultValue = "all") String logType,
            @Parameter(description = "조회할 라인 수")
            @RequestParam(defaultValue = "100") int lines
    ) {
        log.info("[API 호출] GET /api/admin/logs - logType: {}, lines: {}", logType, lines);
        LogResponse response = logService.getLogs(logType, lines);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그 파일 목록 조회
     *
     * @return 로그 파일 목록
     */
    @GetMapping("/files")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "로그 파일 목록", description = "사용 가능한 로그 파일 목록을 조회합니다 (관리자 전용)")
    public ResponseEntity<List<String>> getLogFiles() {
        log.info("[API 호출] GET /api/admin/logs/files");
        List<String> files = logService.getLogFileList();
        return ResponseEntity.ok(files);
    }

    /**
     * 특정 날짜 로그 조회
     *
     * @param logType 로그 타입 (all, error, sql)
     * @param date 날짜 (yyyy-MM-dd)
     * @param lines 조회할 라인 수
     * @return 로그 응답
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "날짜별 로그 조회", description = "특정 날짜의 로그를 조회합니다 (관리자 전용)")
    public ResponseEntity<LogResponse> getLogsByDate(
            @Parameter(description = "로그 타입 (all, error, sql)")
            @RequestParam(defaultValue = "all") String logType,
            @Parameter(description = "날짜 (yyyy-MM-dd)", example = "2025-11-19")
            @PathVariable String date,
            @Parameter(description = "조회할 라인 수")
            @RequestParam(defaultValue = "100") int lines
    ) {
        log.info("[API 호출] GET /api/admin/logs/date/{} - logType: {}, lines: {}", date, logType, lines);
        LogResponse response = logService.getLogsByDate(logType, date, lines);
        return ResponseEntity.ok(response);
    }
}

