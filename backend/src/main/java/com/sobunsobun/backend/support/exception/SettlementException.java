package com.sobunsobun.backend.support.exception;

/**
 * 정산 관련 비즈니스 예외
 *
 * 사용:
 * - throw SettlementException.notFound();
 * - throw SettlementException.amountMismatch(sum, total);
 */
public class SettlementException extends BusinessException {

    public SettlementException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SettlementException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // =================================================
    // 정적 팩토리 메서드
    // =================================================

    public static SettlementException notFound() {
        return new SettlementException(ErrorCode.SETTLEUP_NOT_FOUND);
    }

    public static SettlementException forbidden() {
        return new SettlementException(ErrorCode.SETTLEUP_ACCESS_DENIED);
    }

    public static SettlementException alreadyCompleted() {
        return new SettlementException(ErrorCode.SETTLEUP_ALREADY_COMPLETED);
    }

    public static SettlementException amountMismatch(long participantSum, long totalAmount) {
        return new SettlementException(
                ErrorCode.SETTLEUP_AMOUNT_MISMATCH,
                String.format("참여자별 금액 합계(%d원)가 총 정산 금액(%d원)과 일치하지 않습니다.",
                        participantSum, totalAmount)
        );
    }
}
