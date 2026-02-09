package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 카드 전송 요청 DTO
 *
 * REST API: POST /api/v1/chat/rooms/{roomId}/settlement-card
 * 또는 WebSocket: /app/chat/send (type=SETTLEMENT_CARD)
 *
 * {
 *   "settleUpId": 123,
 *   "roomId": 456
 * }
 *
 * 제약사항:
 * - 방장만 전송 가능
 * - 정산 상태가 COMPLETED일 때만 전송 가능
 * - 이미 전송된 정산은 중복 전송 불가
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendSettlementCardRequest {

    /**
     * 정산 ID
     */
    private Long settleUpId;

    /**
     * 단체 채팅방 ID
     */
    private Long roomId;
}
