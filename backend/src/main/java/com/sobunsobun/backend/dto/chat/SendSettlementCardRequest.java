package com.sobunsobun.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "정산서 카드 전송 요청")
public class SendSettlementCardRequest {

    @NotNull(message = "정산 ID는 필수입니다.")
    @Schema(description = "정산 ID", example = "7")
    private Long settlementId;
}
