package com.sobunsobun.backend.dto.settleup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "정산 응답")
public class SettleUpResponse {

    @Schema(description = "정산 ID", example = "1")
    private Long id;

    @Schema(description = "공동구매 게시글 ID", example = "1")
    private Long groupPostId;

    @Schema(description = "공동구매 게시글 제목", example = "사과 공동구매")
    private String groupPostTitle;

    @Schema(description = "정산 생성자 ID", example = "1")
    private Long settledById;

    @Schema(description = "정산 생성자 닉네임", example = "홍길동")
    private String settledByNickname;

    @Schema(description = "정산 상태 (1: 활성, 2: 비활성, 3: 삭제됨)", example = "1")
    private Integer status;

    @Schema(description = "정산 제목", example = "오늘 저녁 6시 정산")
    private String title;

    @Schema(description = "만남 장소 이름", example = "스타벅스 강남역점")
    private String locationName;

    @Schema(description = "만남 시간 (ISO 8601 형식)", example = "2025-11-21T18:00:00+09:00")
    private LocalDateTime meetAt;

    @Schema(description = "생성 일시 (ISO 8601 형식)", example = "2025-11-20T10:00:00+09:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시 (ISO 8601 형식)", example = "2025-11-20T11:00:00+09:00")
    private LocalDateTime updatedAt;
}

