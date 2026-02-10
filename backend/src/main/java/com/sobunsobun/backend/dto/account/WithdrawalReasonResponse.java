package com.sobunsobun.backend.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 탈퇴 사유 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalReasonResponse {

    /**
     * 탈퇴 사유 ID
     */
    private Long id;

    /**
     * 탈퇴 사유 코드
     */
    private String reasonCode;

    /**
     * 탈퇴 사유 상세
     */
    private String reasonDetail;

    /**
     * 탈퇴 일시
     */
    private LocalDateTime withdrawnAt;

    /**
     * 기록 생성 일시
     */
    private LocalDateTime createdAt;
}
