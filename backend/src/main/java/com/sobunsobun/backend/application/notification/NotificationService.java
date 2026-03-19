package com.sobunsobun.backend.application.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.domain.Notification;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserNotificationSetting;
import com.sobunsobun.backend.dto.common.PageResponse;
import com.sobunsobun.backend.dto.notification.NotificationItemResponse;
import com.sobunsobun.backend.dto.notification.NotificationReadAllResponse;
import com.sobunsobun.backend.dto.notification.NotificationReadResponse;
import com.sobunsobun.backend.dto.notification.UnreadCountResponse;
import com.sobunsobun.backend.infrastructure.firebase.FcmService;
import com.sobunsobun.backend.repository.NotificationRepository;
import com.sobunsobun.backend.repository.UserNotificationSettingRepository;
import com.sobunsobun.backend.support.exception.NotificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    /**
     * 알림 목록 조회 (페이징)
     */
    public PageResponse<NotificationItemResponse> getNotifications(Long userId, int page, int size) {
        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));

        List<NotificationItemResponse> content = result.getContent().stream()
                .map(NotificationItemResponse::from)
                .toList();

        return PageResponse.<NotificationItemResponse>builder()
                .content(content)
                .page(PageResponse.PageInfo.builder()
                        .number(result.getNumber())
                        .size(result.getSize())
                        .totalElements(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .first(result.isFirst())
                        .last(result.isLast())
                        .hasNext(result.hasNext())
                        .hasPrevious(result.hasPrevious())
                        .build())
                .build();
    }

    /**
     * 읽지 않은 알림 수 조회
     */
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return UnreadCountResponse.builder()
                .unreadCount((int) count)
                .build();
    }

    /**
     * 단건 읽음 처리
     */
    @Transactional
    public NotificationReadResponse readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationException::notFound);

        if (!notification.getUser().getId().equals(userId)) {
            throw NotificationException.accessDenied();
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }

        return NotificationReadResponse.builder()
                .id(notification.getId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .build();
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public NotificationReadAllResponse readAll(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        LocalDateTime now = LocalDateTime.now();
        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);

        log.info("✅ 전체 읽음 처리 완료 - userId: {}, count: {}", userId, unread.size());

        return NotificationReadAllResponse.builder()
                .updatedCount(unread.size())
                .readAt(now)
                .build();
    }

    /**
     * 알림 DB 저장 + FCM 발송 (내부 헬퍼)
     * 각 도메인 서비스에서 호출
     */
    @Transactional
    public void createAndSend(User recipient, String type, String title, String body, Map<String, String> data) {
        // 1. 알림 DB 저장
        String dataPayload = serializeData(data);
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .body(body)
                .dataPayload(dataPayload)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // 2. 푸시 알림 전송 (설정 확인 후)
        boolean pushEnabled = userNotificationSettingRepository.findByUserId(recipient.getId())
                .map(UserNotificationSetting::getPushEnabled)
                .orElse(true); // 설정 없으면 기본값 true

        if (pushEnabled) {
            fcmService.sendToUser(recipient.getId(), title, body, data);
        }
    }

    private String serializeData(Map<String, String> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("알림 data 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
