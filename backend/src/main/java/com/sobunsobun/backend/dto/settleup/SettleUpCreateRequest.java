package com.sobunsobun.backend.dto.settleup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "정산 생성 요청")
public class SettleUpCreateRequest {

    @NotNull(message = "공동구매 게시글 ID는 필수입니다")
    @Schema(description = "공동구매 게시글 ID", example = "1")
    private Long groupPostId;

    @Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다")
    @Schema(description = "정산 제목", example = "오늘 저녁 6시 정산", maxLength = 50)
    private String title;

    @Size(max = 120, message = "장소명은 최대 120자까지 입력 가능합니다")
    @Schema(description = "만남 장소 이름", example = "스타벅스 강남역점", maxLength = 120)
    private String locationName;

    @Schema(description = "만남 시간 (ISO 8601 형식 지원)", example = "2025-11-21T18:00:00+09:00")
    private LocalDateTime meetAt;
}

