package com.sobunsobun.backend.dto.settleup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "정산 품목 요청")
public class SettlementItemRequest {

    @NotBlank(message = "품목명은 필수입니다.")
    @Schema(description = "품목명", example = "사과")
    private String itemName;

    @Schema(description = "수량 또는 중량", example = "2.0")
    private BigDecimal quantity;

    @Schema(description = "단위 (개, g, kg, ml 등)", example = "개")
    private String unit;

    @NotNull(message = "품목 금액은 필수입니다.")
    @Positive(message = "품목 금액은 0보다 커야 합니다.")
    @Schema(description = "품목 금액 (원)", example = "10000")
    private Long amount;
}
