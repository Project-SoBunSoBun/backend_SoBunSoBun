package com.sobunsobun.backend.domain;

/**
 * 정산 상태 코드
 */
public class SettleUpStatus {

    /**
     * 미정산 상태
     */
    public static final Integer UNSETTLED = 1;

    /**
     * 정산완료 상태
     */
    public static final Integer SETTLED = 2;

    /**
     * 삭제됨 상태
     */
    public static final Integer DELETED = 3;

    private SettleUpStatus() {
        // 인스턴스화 방지
    }
}

