package com.sobunsobun.backend.domain;

/**
 * 정산 상태 코드
 */
public class SettleUpStatus {

    /**
     * 활성 상태
     */
    public static final Integer ACTIVE = 1;

    /**
     * 비활성 상태
     */
    public static final Integer INACTIVE = 2;

    /**
     * 삭제됨 상태
     */
    public static final Integer DELETED = 3;

    private SettleUpStatus() {
        // 인스턴스화 방지
    }
}

