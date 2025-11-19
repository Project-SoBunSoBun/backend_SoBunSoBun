package com.sobunsobun.backend.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 로그 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponse {
    private String logType;        // 로그 타입 (all, error, sql)
    private String fileName;       // 로그 파일명
    private long totalLines;       // 전체 라인 수
    private int requestedLines;    // 요청한 라인 수
    private List<String> logs;     // 로그 내용
}

