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
@Schema(description = "정산 완료 요청 (iOS 최종 계산 결과)")
public class SettlementCompleteRequest {

    @NotNull(message = "총 정산 금액은 필수입니다.")
    @Positive(message = "총 정산 금액은 0보다 커야 합니다.")
    @Schema(description = "총 정산 금액 (원). 참여자별 assignedAmount 합계와 반드시 일치해야 합니다.", example = "45000")
    private Long totalAmount;

    @NotEmpty(message = "참여자 목록은 최소 1명 이상이어야 합니다.")
    @Valid
    @Schema(description = "참여자별 금액 및 품목 내역")
    private List<SettlementParticipantRequest> participants;
}
