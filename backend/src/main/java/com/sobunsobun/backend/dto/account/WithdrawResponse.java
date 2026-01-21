package com.sobunsobun.backend.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 탈퇴 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawResponse {

    private String message;
    private LocalDateTime withdrawnAt;

    /**
     * 개인정보 보관 기간 (일)
     */
    private Integer dataRetentionDays;
}

