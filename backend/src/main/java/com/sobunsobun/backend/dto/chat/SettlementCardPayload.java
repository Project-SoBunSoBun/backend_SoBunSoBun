package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 카드 응답 DTO
 *
 * 단체 채팅에서 정산이 COMPLETED일 때만 표시
 * 방장만 "정산서 카드" 전송 가능
 *
 * WebSocket: /app/chat/send (SETTLEMENT_CARD type)
 * cardPayload: JSON 형식
 *
 * {
 *   "settleUpId": 123,
 *   "status": "COMPLETED",
 *   "totalAmount": 100000,
 *   "participants": [
 *     {
 *       "userId": 456,
 *       "nickname": "User1",
 *       "amount": 50000,
 *       "isPaid": true
 *     }
 *   ],
 *   "completedAt": "2025-01-27T10:30:00"
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementCardPayload {

    /**
     * 정산 ID
     */
    private Long settleUpId;

    /**
     * 정산 상태 (COMPLETED)
     */
    private String status;

    /**
     * 총 정산 금액
     */
    private BigDecimal totalAmount;

    /**
     * 참여자 목록
     */
    private List<ParticipantInfo> participants;

    /**
     * 정산 완료 시간
     */
    private LocalDateTime completedAt;

    /**
     * 참여자 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String nickname;
        private BigDecimal amount;
        private Boolean isPaid;
    }
}
