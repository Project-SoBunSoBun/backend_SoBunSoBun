package com.sobunsobun.backend.scheduler;

import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomStatus;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomCleanupScheduler {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 만료된 채팅방 삭제 스케줄러
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredChatRooms() {
        log.info("[스케줄러] 만료된 채팅방 삭제 작업 시작");

        Instant now = Instant.now();

        // 1. 만료된 채팅방 조회 (status = CLOSED, expireAt < 현재 시간)
        List<ChatRoom> expiredRooms = chatRoomRepository.findByStatusAndExpireAtBefore(
                ChatRoomStatus.CLOSED, now);

        if (expiredRooms.isEmpty()) {
            log.info("[스케줄러] 삭제할 만료된 채팅방이 없습니다.");
            return;
        }

        log.info("[스케줄러] 만료된 채팅방 {}개 발견", expiredRooms.size());

        List<Long> expiredRoomIds = expiredRooms.stream()
                .map(ChatRoom::getId)
                .toList();

        // 2. ChatMember 삭제 (하드 삭제)
        chatMemberRepository.deleteByRoomIdIn(expiredRoomIds);
        log.info("[스케줄러] ChatMember 삭제 완료 - 채팅방 ID: {}", expiredRoomIds);

        // 3. MongoDB 메시지 삭제
        int deletedMessageCount = 0;
        for (Long roomId : expiredRoomIds) {
            try {
                chatMessageRepository.deleteByRoomId(roomId);
                deletedMessageCount++;
                log.debug("[스케줄러] MongoDB 메시지 삭제 - 채팅방 ID: {}", roomId);
            } catch (Exception e) {
                log.error("[스케줄러] MongoDB 메시지 삭제 실패 - 채팅방 ID: {}, 오류: {}", roomId, e.getMessage());
            }
        }
        log.info("[스케줄러] MongoDB 메시지 삭제 완료 - 성공: {}/{}", deletedMessageCount, expiredRoomIds.size());

        // 4. ChatRoom 삭제 (하드 삭제)
        chatRoomRepository.deleteAllById(expiredRoomIds);
        log.info("[스케줄러] ChatRoom 삭제 완료 - 채팅방 ID: {}", expiredRoomIds);

        log.info("[스케줄러] 만료된 채팅방 삭제 작업 완료 - 삭제된 채팅방 개수: {}", expiredRooms.size());
    }
}
