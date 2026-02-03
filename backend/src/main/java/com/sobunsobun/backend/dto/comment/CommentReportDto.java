package com.sobunsobun.backend.dto.comment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sobunsobun.backend.domain.ReportReason;
import com.sobunsobun.backend.domain.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CommentReportDto {

    /**
     * 신고 생성 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateRequest2 {

        @NotNull(message = "댓글 ID는 필수입니다")
        @Schema(
            description = "신고할 댓글의 ID",
            example = "1",
            required = true,
            type = "integer",
            format = "int64"
        )
        @JsonProperty("commentId")
        private Long commentId;

        @NotNull(message = "신고 사유는 필수입니다")
        @Schema(
            description = "신고 사유",
            example = "SPAM",
            required = true,
            allowableValues = {"SPAM", "ABUSE", "INAPPROPRIATE", "FRAUD", "HARMFUL", "SCAM", "OTHER"},
            type = "string"
        )
        @JsonProperty("reason")
        private ReportReason reason;

        @Size(max = 1000, message = "상세 내용은 1000자 이내여야 합니다")
        @Schema(
            description = "신고 상세 설명",
            example = "부적절한 내용입니다!!",
            maxLength = 1000,
            type = "string"
        )
        @JsonProperty("description")
        private String description;

        /**
         * Jackson JSON 역직렬화를 위한 생성자
         */
        @JsonCreator
        public CreateRequest2(
                @JsonProperty("commentId") Long commentId,
                @JsonProperty("reason") ReportReason reason,
                @JsonProperty("description") String description) {
            this.commentId = commentId;
            this.reason = reason;
            this.description = description;
        }
    }

    /**
     * 신고 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long commentId;
        private Long userId;
        private String userName;
        private ReportReason reason;
        private String description;
        private ReportStatus status;
        private String resolution;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime handledAt;
    }

    /**
     * 신고 목록 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private Long id;
        private Long commentId;
        private String commentContent;
        private Long userId;
        private String userName;
        private ReportReason reason;
        private ReportStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime handledAt;
    }

    /**
     * 신고 상태 업데이트 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateStatusRequest {
        @NotNull(message = "신고 상태는 필수입니다")
        private ReportStatus status;

        @Size(max = 255, message = "처리 결과는 255자 이내여야 합니다")
        private String resolution;
    }

    /**
     * 신고 통계 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatisticsResponse {
        private Long commentId;
        private long totalReports;
        private long pendingReports;
        private long reviewingReports;
        private long resolvedReports;
    }
}
