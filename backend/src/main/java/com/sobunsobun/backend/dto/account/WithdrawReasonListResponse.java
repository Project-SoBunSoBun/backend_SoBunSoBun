package com.sobunsobun.backend.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 탈퇴 사유 목록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawReasonListResponse {

    private List<WithdrawReasonItem> reasons;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WithdrawReasonItem {
        /**
         * 사유 코드
         */
        private String code;

        /**
         * 사유 라벨 (표시용)
         */
        private String label;
    }
}

