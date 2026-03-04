package com.sobunsobun.backend.dto.settleup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "정산 참여자 요청")
public class SettlementParticipantRequest {

    @NotNull(message = "참여자 ID는 필수입니다.")
    @Schema(description = "참여자 사용자 ID", example = "101")
    private Long userId;

    @NotNull(message = "참여자 부담 금액은 필수입니다.")
    @Positive(message = "부담 금액은 0보다 커야 합니다.")
    @Schema(description = "이 참여자의 총 부담 금액 (원)", example = "15000")
    private Long assignedAmount;

    @NotEmpty(message = "품목 목록은 최소 1개 이상이어야 합니다.")
    @Valid
    @Schema(description = "이 참여자에게 할당된 품목 목록")
    private List<SettlementItemRequest> items;
}
