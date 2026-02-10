package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.WithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 회원 탈퇴 사유 기록 레포지토리
 */
@Repository
public interface WithdrawalReasonRepository extends JpaRepository<WithdrawalReason, Long> {

    /**
     * 사용자 ID로 탈퇴 사유 조회
     */
    Optional<WithdrawalReason> findByUserId(Long userId);
}
