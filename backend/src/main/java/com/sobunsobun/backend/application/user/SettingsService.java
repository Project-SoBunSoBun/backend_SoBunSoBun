package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserNotificationSetting;
import com.sobunsobun.backend.dto.settings.NotificationSettingRequest;
import com.sobunsobun.backend.dto.settings.NotificationSettingResponse;
import com.sobunsobun.backend.repository.UserNotificationSettingRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final UserRepository userRepository;

    /**
     * 알림 설정 조회
     * 설정이 없으면 기본값 반환
     */
    public NotificationSettingResponse getNotificationSetting(Long userId) {
        return userNotificationSettingRepository.findByUserId(userId)
                .map(s -> NotificationSettingResponse.builder()
                        .pushEnabled(s.getPushEnabled())
                        .chatEnabled(s.getChatEnabled())
                        .marketingEnabled(s.getMarketingEnabled())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .orElseGet(() -> NotificationSettingResponse.builder()
                        .pushEnabled(true)
                        .chatEnabled(true)
                        .marketingEnabled(false)
                        .build());
    }

    /**
     * 알림 설정 변경 (upsert)
     * null 필드는 현재 값 유지
     */
    @Transactional
    public NotificationSettingResponse updateNotificationSetting(Long userId, NotificationSettingRequest request) {
        UserNotificationSetting setting = userNotificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow(UserException::notFound);
                    return UserNotificationSetting.builder()
                            .user(user)
                            .pushEnabled(true)
                            .chatEnabled(true)
                            .marketingEnabled(false)
                            .build();
                });

        if (request.getPushEnabled() != null) {
            setting.setPushEnabled(request.getPushEnabled());
        }
        if (request.getChatEnabled() != null) {
            setting.setChatEnabled(request.getChatEnabled());
        }
        if (request.getMarketingEnabled() != null) {
            setting.setMarketingEnabled(request.getMarketingEnabled());
        }

        UserNotificationSetting saved = userNotificationSettingRepository.save(setting);
        log.info(" 알림 설정 변경 완료 - userId: {}", userId);

        return NotificationSettingResponse.builder()
                .pushEnabled(saved.getPushEnabled())
                .chatEnabled(saved.getChatEnabled())
                .marketingEnabled(saved.getMarketingEnabled())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
