package com.sobunsobun.backend.application.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 채팅 관련 로깅 유틸리티
 *
 * 모든 채팅 로그를 일관된 형식으로 출력합니다.
 */
@Slf4j
@Component
public class ChatLoggingUtil {

    // ========== Success Logs ==========
    public void logMessageSaved(Long messageId, Long roomId) {
        log.info("✅ 메시지 저장 완료 - messageId: {}, roomId: {}", messageId, roomId);
    }

    public void logMessageBroadcastStarted(String destination) {
        log.info("📢 브로드캐스팅 시작 - destination: {}", destination);
    }

    public void logMessageBroadcastCompleted(Long messageId, String destination) {
        log.info("✅ 메시지 브로드캐스트 완료 - messageId: {}, destination: {}", messageId, destination);
    }

    public void logAuthorizationSuccess(Long roomId, Long userId) {
        log.info("✅ 권한 검증 성공 - roomId: {}, userId: {}", roomId, userId);
    }

    public void logChatRoomFound(Long roomId, String name, int memberCount) {
        log.info("📍 채팅방 조회 완료 - roomId: {}, name: {}, members: {}",
                roomId, name, memberCount);
    }

    // ========== Info Logs ==========
    public void logMessageSaveStarted(Long roomId, Long senderId, String type) {
        log.info("💾 메시지 저장 시작 - roomId: {}, senderId: {}, type: {}",
                roomId, senderId, type);
    }

    public void logUserIdExtracted(Long userId, String source) {
        log.debug("✅ userId 추출 - source: {}, userId: {}", source, userId);
    }

    // ========== Warning Logs ==========
    public void logAuthorizationFailed(Long roomId, Long userId) {
        log.warn("❌ 채팅방 접근 권한 없음 - roomId: {}, userId: {}", roomId, userId);
    }

    public void logUserIdExtractionFailed(String source, String value) {
        log.warn("⚠️ userId 추출 실패 - source: {}, value: {} (숫자가 아님)", source, value);
    }

    public void logNullParameter(String paramName) {
        log.warn("⚠️ {} 값이 null입니다", paramName);
    }

    // ========== Error Logs ==========
    public void logChatRoomNotFound(Long roomId) {
        log.error("❌ 채팅방을 찾을 수 없음 - roomId: {}", roomId);
    }

    public void logBroadcastFailed(String destination, String errorMessage) {
        log.error("❌ 브로드캐스팅 중 오류 발생 - destination: {}, error: {}",
                destination, errorMessage);
    }

    public void logMessageSaveFailed(Long roomId, Long userId, String errorMessage) {
        log.error("❌ 메시지 저장 실패 - roomId: {}, userId: {}, error: {}",
                roomId, userId, errorMessage);
    }

    public void logMessageQueryFailed(Long roomId, String errorMessage) {
        log.error("❌ 메시지 조회 실패 - roomId: {}, error: {}", roomId, errorMessage);
    }
}
