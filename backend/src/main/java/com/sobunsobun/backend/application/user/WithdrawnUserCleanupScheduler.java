package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserStatus;
import com.sobunsobun.backend.repository.WithdrawalReasonRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 사용자 데이터 정리 스케줄러
 *
 * 90일이 경과한 탈퇴 사용자의 데이터를 완전히 삭제(Hard Delete)합니다.
 * - 매일 새벽 3시에 실행
 * - reactivatableAt이 현재 시각 이전인 DELETED 상태의 User를 대상으로 함
 * - WithdrawalReason 레코드 명시 삭제
 * - AuthProvider는 User 엔티티의 CascadeType.ALL + orphanRemoval로 자동 삭제
 * - User 레코드를 최종적으로 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserCleanupScheduler {

    private final UserRepository userRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;

    /**
     * 90일 경과한 탈퇴 사용자 데이터 정리
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        log.info("🧹 탈퇴 사용자 데이터 정리 스케줄러 시작");

        LocalDateTime now = LocalDateTime.now();

        // 재가입 가능 일시가 현재보다 이전인 DELETED 사용자 조회
        List<User> expiredUsers = userRepository.findByStatusAndReactivatableAtBefore(
                UserStatus.DELETED, now);

        if (expiredUsers.isEmpty()) {
            log.info("🧹 정리 대상 탈퇴 사용자 없음");
            return;
        }

        log.info("🧹 정리 대상 탈퇴 사용자 {}명 발견", expiredUsers.size());

        int successCount = 0;
        int failCount = 0;

        for (User user : expiredUsers) {
            try {
                Long userId = user.getId();
                log.info("🧹 탈퇴 사용자 데이터 삭제 시작 - 사용자 ID: {}, 탈퇴일: {}, 재가입 가능일: {}",
                        userId, user.getWithdrawnAt(), user.getReactivatableAt());

                // 1. WithdrawalReason 삭제 (User FK 참조)
                withdrawalReasonRepository.deleteByUserId(userId);

                // 2. User 레코드 삭제 (AuthProvider는 cascade로 자동 삭제됨)
                userRepository.delete(user);

                successCount++;
                log.info("✅ 탈퇴 사용자 데이터 삭제 완료 - 사용자 ID: {}", userId);
            } catch (Exception e) {
                failCount++;
                log.error("❌ 탈퇴 사용자 데이터 삭제 실패 - 사용자 ID: {}, 오류: {}",
                        user.getId(), e.getMessage(), e);
            }
        }

        log.info("🧹 탈퇴 사용자 데이터 정리 완료 - 성공: {}건, 실패: {}건", successCount, failCount);
    }
}
