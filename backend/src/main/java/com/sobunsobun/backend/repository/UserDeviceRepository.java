package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 디바이스 (FCM 토큰) 리포지토리
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    /**
     * 사용자 ID와 디바이스 ID로 조회
     */
    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    /**
     * FCM 토큰으로 조회
     */
    Optional<UserDevice> findByFcmToken(String fcmToken);

    /**
     * 특정 사용자의 모든 디바이스 정보 삭제 (회원탈퇴용)
     */
    void deleteByUserId(Long userId);
}

