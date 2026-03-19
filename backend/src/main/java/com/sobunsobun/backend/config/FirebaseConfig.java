package com.sobunsobun.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${firebase.project-id}")
    private String projectId;

    /**
     * Firebase 초기화
     * FIREBASE_CREDENTIALS_JSON 환경변수(Base64)가 없으면 FCM 비활성화 상태로 기동
     */
    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        if (credentialsJson == null || credentialsJson.isBlank()) {
            log.warn(" FIREBASE_CREDENTIALS_JSON 환경변수가 없습니다. FCM이 비활성화됩니다.");
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsJson.trim());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decoded)))
                    .setProjectId(projectId)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info(" Firebase 초기화 완료 - projectId: {}", projectId);
            return app;
        } catch (Exception e) {
            log.warn(" Firebase 초기화 실패 (FCM 비활성화됨): {}", e.getMessage());
            return null;
        }
    }
}
