package com.sobunsobun.backend.dto.settleup;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "정산 수정 요청")
public class SettleUpUpdateRequest {

    @Schema(description = "정산 상태 (1: 활성, 2: 비활성, 3: 삭제됨)", example = "1")
    private Integer status;

    @Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다")
    @Schema(description = "정산 제목", example = "내일 오후 3시 정산", maxLength = 50)
    private String title;

    @Size(max = 120, message = "장소명은 최대 120자까지 입력 가능합니다")
    @Schema(description = "만남 장소 이름", example = "카페 베네 신촌점", maxLength = 120)
    private String locationName;

    @Schema(description = "만남 시간 (ISO 8601 형식 지원)", example = "2025-11-22T15:00:00+09:00")
    private LocalDateTime meetAt;
}

