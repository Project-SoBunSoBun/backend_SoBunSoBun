package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserDevice;
import com.sobunsobun.backend.dto.device.DeviceDeleteResponse;
import com.sobunsobun.backend.dto.device.DeviceRegistrationRequest;
import com.sobunsobun.backend.dto.device.DeviceRegistrationResponse;
import com.sobunsobun.backend.dto.device.DeviceUpdateRequest;
import com.sobunsobun.backend.dto.device.DeviceUpdateResponse;
import com.sobunsobun.backend.repository.UserDeviceRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    /**
     * FCM 토큰 등록 또는 갱신 (upsert)
     * - 동일 userId + deviceId가 있으면 토큰 갱신
     * - 없으면 신규 저장
     * - 동일 fcmToken이 다른 user에게 있으면 이전 레코드 삭제 후 저장 (기기 이전)
     */
    public DeviceRegistrationResponse registerDevice(Long userId, DeviceRegistrationRequest request) {
        // 다른 사용자에게 같은 FCM 토큰이 등록된 경우 삭제 (기기 이전)
        userDeviceRepository.findByFcmToken(request.getFcmToken()).ifPresent(existing -> {
            if (!existing.getUser().getId().equals(userId)) {
                log.info("📵 기기 이전: 기존 토큰 삭제 - userId: {}, deviceId: {}",
                        existing.getUser().getId(), existing.getDeviceId());
                userDeviceRepository.delete(existing);
            }
        });

        // 기존 디바이스 조회 (upsert)
        UserDevice device = userDeviceRepository
                .findByUserIdAndDeviceId(userId, request.getDeviceId())
                .map(existing -> {
                    existing.updateToken(request.getFcmToken(), request.getAppVersion(), request.getOsVersion());
                    existing.setEnabled(true);
                    return existing;
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow(UserException::notFound);
                    return UserDevice.builder()
                            .user(user)
                            .deviceId(request.getDeviceId())
                            .fcmToken(request.getFcmToken())
                            .platform(request.getPlatform())
                            .appVersion(request.getAppVersion())
                            .osVersion(request.getOsVersion())
                            .isEnabled(true)
                            .lastSeenAt(LocalDateTime.now())
                            .build();
                });

        UserDevice saved = userDeviceRepository.save(device);
        log.info("✅ FCM 토큰 등록/갱신 완료 - userId: {}, deviceId: {}", userId, saved.getDeviceId());

        return DeviceRegistrationResponse.builder()
                .deviceId(saved.getDeviceId())
                .registered(true)
                .isEnabled(saved.getIsEnabled())
                .registeredAt(saved.getUpdatedAt())
                .build();
    }

    /**
     * FCM 토큰 삭제
     */
    public DeviceDeleteResponse deleteDevice(Long userId, String deviceId) {
        userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId).ifPresent(device -> {
            userDeviceRepository.delete(device);
            log.info("✅ FCM 토큰 삭제 완료 - userId: {}, deviceId: {}", userId, deviceId);
        });

        return DeviceDeleteResponse.builder()
                .deviceId(deviceId)
                .deleted(true)
                .build();
    }

    /**
     * FCM 토큰 활성화/비활성화
     */
    public DeviceUpdateResponse updateDevice(Long userId, String deviceId, DeviceUpdateRequest request) {
        UserDevice device = userDeviceRepository
                .findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("디바이스를 찾을 수 없습니다."));

        device.setEnabled(request.getIsEnabled());
        UserDevice saved = userDeviceRepository.save(device);
        log.info("✅ FCM 토큰 상태 변경 완료 - userId: {}, deviceId: {}, enabled: {}",
                userId, deviceId, request.getIsEnabled());

        return DeviceUpdateResponse.builder()
                .deviceId(saved.getDeviceId())
                .isEnabled(saved.getIsEnabled())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
