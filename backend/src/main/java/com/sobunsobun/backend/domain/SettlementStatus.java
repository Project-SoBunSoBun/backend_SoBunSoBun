package com.sobunsobun.backend.domain;

/**
 * 정산 상태 Enum
 *
 * PENDING   : 미완료 (게시글 생성 시 자동 생성)
 * COMPLETED : 완료   (iOS 클라이언트가 최종 계산 결과 전송 후)
 */
public enum SettlementStatus {
    PENDING,
    COMPLETED
}
