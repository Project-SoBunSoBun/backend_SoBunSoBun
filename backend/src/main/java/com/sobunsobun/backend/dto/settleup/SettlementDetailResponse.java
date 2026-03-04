package com.sobunsobun.backend.dto.settleup;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sobunsobun.backend.domain.Settlement;
import com.sobunsobun.backend.domain.SettlementItem;
import com.sobunsobun.backend.domain.SettlementParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 상세 조회 및 완료 응답 (참여자 + 품목 포함)
 */
@Getter
@Builder
@Schema(description = "정산 상세 응답")
public class SettlementDetailResponse {

    @Schema(description = "정산 ID", example = "7")
    private Long id;

    @Schema(description = "게시글 ID", example = "5")
    private Long groupPostId;

    @Schema(description = "게시글 제목", example = "마트 공동구매")
    private String groupPostTitle;

    @Schema(description = "정산 상태", example = "COMPLETED")
    private String status;

    @Schema(description = "총 정산 금액", example = "45000")
    private Long totalAmount;

    @Schema(description = "참여자별 내역")
    private List<ParticipantResponse> participants;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;

    public static SettlementDetailResponse from(Settlement settlement) {
        return SettlementDetailResponse.builder()
                .id(settlement.getId())
                .groupPostId(settlement.getGroupPost().getId())
                .groupPostTitle(settlement.getGroupPost().getTitle())
                .status(settlement.getStatus().name())
                .totalAmount(settlement.getTotalAmount())
                .participants(settlement.getParticipants().stream()
                        .map(ParticipantResponse::from)
                        .toList())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }

    // ── 중첩 DTO ──────────────────────────────────────

    @Getter
    @Builder
    @Schema(description = "참여자 상세")
    public static class ParticipantResponse {
        private Long id;
        private Long userId;
        private String userNickname;
        private Long assignedAmount;
        private List<ItemResponse> items;

        public static ParticipantResponse from(SettlementParticipant p) {
            return ParticipantResponse.builder()
                    .id(p.getId())
                    .userId(p.getUser().getId())
                    .userNickname(p.getUser().getNickname())
                    .assignedAmount(p.getAssignedAmount())
                    .items(p.getItems().stream().map(ItemResponse::from).toList())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "품목 상세")
    public static class ItemResponse {
        private Long id;
        private String itemName;
        private BigDecimal quantity;
        private String unit;
        private Long amount;

        public static ItemResponse from(SettlementItem item) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .itemName(item.getItemName())
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .amount(item.getAmount())
                    .build();
        }
    }
}
