package com.sobunsobun.backend.infrastructure.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.sobunsobun.backend.domain.UserDevice;
import com.sobunsobun.backend.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserDeviceRepository userDeviceRepository;

    /**
     * 단일 FCM 토큰으로 푸시 알림 발송
     */
    @Async("fcmTaskExecutor")
    public void sendToToken(String fcmToken, String title, String body, Map<String, String> data, int badgeCount, String apnsCategory) {
        if (!isFirebaseAvailable()) return;
        if (fcmToken == null || fcmToken.isBlank()) return;

        try {
            Aps.Builder apsBuilder = Aps.builder().setBadge(badgeCount).setMutableContent(true);
            if (apnsCategory != null && !apnsCategory.isBlank()) {
                apsBuilder.setCategory(apnsCategory);
            }

            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(apsBuilder.build())
                            .build());

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.debug(" FCM 발송 성공: messageId={}", response);

        } catch (FirebaseMessagingException e) {
            handleFirebaseException(fcmToken, e);
        } catch (Exception e) {
            log.warn(" FCM 발송 중 예외 발생: token={}, error={}", fcmToken, e.getMessage());
        }
    }

    /**
     * 특정 사용자의 모든 활성 디바이스에 푸시 알림 발송
     */
    @Async("fcmTaskExecutor")
    public void sendToUser(Long userId, String title, String body, Map<String, String> data, int badgeCount, String apnsCategory) {
        if (!isFirebaseAvailable()) return;

        List<UserDevice> devices = userDeviceRepository.findByUserIdAndIsEnabledTrue(userId);
        if (devices.isEmpty()) {
            log.info(" 활성 디바이스 없음 - userId: {}", userId);
            return;
        }

        log.info(" FCM 발송 시작 - userId: {}, deviceCount: {}, badgeCount: {}", userId, devices.size(), badgeCount);

        Aps.Builder apsBuilder = Aps.builder().setBadge(badgeCount).setMutableContent(true);
        if (apnsCategory != null && !apnsCategory.isBlank()) {
            apsBuilder.setCategory(apnsCategory);
        }
        Aps aps = apsBuilder.build();

        for (UserDevice device : devices) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(device.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(aps)
                                .build());

                if (data != null && !data.isEmpty()) {
                    builder.putAllData(data);
                }

                String response = FirebaseMessaging.getInstance().send(builder.build());
                log.info(" FCM 발송 성공: userId={}, deviceId={}, messageId={}",
                        userId, device.getDeviceId(), response);

            } catch (FirebaseMessagingException e) {
                handleFirebaseExceptionForDevice(device, e);
            } catch (Exception e) {
                log.warn(" FCM 발송 중 예외: userId={}, deviceId={}, error={}",
                        userId, device.getDeviceId(), e.getMessage());
            }
        }
    }

    private void handleFirebaseException(String fcmToken, FirebaseMessagingException e) {
        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            log.warn(" FCM 토큰 만료됨 - token={}", fcmToken);
            userDeviceRepository.findByFcmToken(fcmToken).ifPresent(device -> {
                device.setEnabled(false);
                userDeviceRepository.save(device);
                log.info(" 디바이스 비활성화 완료 - deviceId={}", device.getDeviceId());
            });
        } else {
            log.warn(" FCM 발송 실패: errorCode={}, message={}", e.getMessagingErrorCode(), e.getMessage());
        }
    }

    private void handleFirebaseExceptionForDevice(UserDevice device, FirebaseMessagingException e) {
        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            log.warn(" FCM 토큰 만료 - userId={}, deviceId={}", device.getUser().getId(), device.getDeviceId());
            device.setEnabled(false);
            userDeviceRepository.save(device);
        } else {
            log.warn(" FCM 발송 실패: userId={}, deviceId={}, errorCode={}, message={}",
                    device.getUser().getId(), device.getDeviceId(), e.getMessagingErrorCode(), e.getMessage());
        }
    }

    private boolean isFirebaseAvailable() {
        try {
            return !FirebaseApp.getApps().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
