package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.*;
import com.sobunsobun.backend.dto.chat.ChatMessageDto;
import com.sobunsobun.backend.dto.chat.MessageResponse;
import com.sobunsobun.backend.infrastructure.redis.ChatRedisService;
import com.sobunsobun.backend.infrastructure.redis.RedisPublisher;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final RedisPublisher redisPublisher;  // Redis Pub/Sub을 위한 Publisher
    private final ChatRedisService chatRedisService;  // Redis 상태 관리 서비스

    /**
     * 메시지 저장
     *
     * 1. 채팅방 멤버 권한 확인
     * 2. 메시지 DB 저장
     * 3. 채팅방 마지막 메시지 정보 업데이트
     * 4. DTO 반환
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse saveMessage(
            Long roomId,
            Long senderId,
            ChatMessageType type,
            String content,
            String imageUrl,
            String cardPayload
    ) {
        try {
            log.info("📝 [메시지 저장 시작] roomId: {}, senderId: {}, type: {}, contentLength: {}",
                    roomId, senderId, type, content != null ? content.length() : 0);

            // 1. 채팅방 조회 및 권한 검증
            log.debug("🔍 [단계1] 채팅방 조회 중... roomId: {}", roomId);
            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> {
                        log.error("❌ [단계1 실패] 채팅방을 찾을 수 없음: roomId={}", roomId);
                        return new RuntimeException("Chat room not found: " + roomId);
                    });
            log.debug("✅ [단계1 성공] 채팅방 조회됨: roomId={}, roomName={}", chatRoom.getId(), chatRoom.getName());

            int memberCount = chatRoom.getMembers() != null ? chatRoom.getMembers().size() : 0;
            log.debug("✅ [단계1] 멤버 정보 로드됨: memberCount={}, members={}",
                    memberCount,
                    chatRoom.getMembers() != null ?
                            chatRoom.getMembers().stream()
                                    .map(m -> m.getUser().getId() + ":" + m.getStatus())
                                    .toList() : "null");

            // 권한 검증 (메모리 + DB 이중 확인)
            boolean isMemberInMemory = chatRoom.isMember(senderId);
            boolean isMemberInDb = chatMemberRepository.isActiveMember(roomId, senderId);

            log.debug("🔐 [권한 검증] userId={}, roomId={}", senderId, roomId);
            log.debug("   메모리 확인: {}, DB 확인: {}", isMemberInMemory, isMemberInDb);

            if (!isMemberInDb) {
                log.error("❌ [권한 검증 실패] userId {}는 roomId {} 멤버가 아님 (DB 확인)", senderId, roomId);
                // 디버깅 정보 출력
                if (chatRoom.getMembers() != null) {
                    log.error("   현재 멤버 목록: {}", chatRoom.getMembers().stream()
                            .map(m -> "u" + m.getUser().getId() + "(" + m.getStatus() + ")")
                            .toList());
                }
                throw new RuntimeException("채팅방에 초대되지 않은 사용자입니다");
            }
            log.debug("✅ [권한 검증 성공] userId {}는 roomId {} 멤버임 (DB 확인)", senderId, roomId);

            // 2. 사용자 조회
            log.debug("🔍 [단계2] 사용자 조회 중... senderId: {}", senderId);
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> {
                        log.error("❌ [단계2 실패] 사용자를 찾을 수 없음: senderId={}", senderId);
                        return new RuntimeException("User not found: " + senderId);
                    });
            log.debug("✅ [단계2 성공] 사용자 조회됨: senderId={}, nickname={}", sender.getId(), sender.getNickname());

            // 3. 메시지 생성 및 저장
            log.debug("🔍 [단계3] 메시지 엔티티 생성 중...");
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(sender)
                    .type(type)
                    .content(content)
                    .imageUrl(imageUrl)
                    .cardPayload(cardPayload)
                    .readCount(0)
                    .build();
            log.debug("✅ [단계3] 메시지 엔티티 생성됨");

            log.debug("💾 [단계3] 메시지를 DB에 저장 중...");
            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("✅ [단계3 성공] 메시지 DB 저장 완료: messageId={}, createdAt={}",
                    savedMessage.getId(), savedMessage.getCreatedAt());

            // 4. 채팅방 메타데이터 업데이트
            log.debug("🔍 [단계4] 채팅방 메타데이터 업데이트 중...");
            chatRoom.setLastMessageAt(savedMessage.getCreatedAt());
            chatRoom.setLastMessagePreview(truncateContent(content));
            chatRoom.setLastMessageSenderId(senderId);

            log.debug("💾 [단계4] 채팅방 정보 DB에 저장 중...");
            chatRoomRepository.save(chatRoom);
            log.info("✅ [단계4 성공] 채팅방 메타데이터 업데이트 완료: lastMessageAt={}", chatRoom.getLastMessageAt());

            // 5. Redis Pub/Sub을 통해 메시지 발행
            log.debug("🔄 [단계5] Redis 메시지 발행 중...");
            try {
                // createdAt을 ISO 8601 형식으로 변환 (KST, +09:00)
                String createdAtIso = savedMessage.getCreatedAt()
                        .atZone(java.time.ZoneId.of("Asia/Seoul"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

                ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                        .type(savedMessage.getType())
                        .roomId(roomId)
                        .senderId(senderId)
                        .senderName(sender.getNickname())
                        .message(savedMessage.getContent())
                        .imageUrl(savedMessage.getImageUrl())
                        .cardPayload(savedMessage.getCardPayload())
                        .messageId(savedMessage.getId())
                        .timestamp(System.currentTimeMillis())
                        // REST API 커서 조회 응답과 동일한 필드 추가
                        .id(savedMessage.getId().toString())
                        .nickname(sender.getNickname())
                        .profileImage(sender.getProfileImageUrl())
                        .senderProfileImageUrl(sender.getProfileImageUrl())
                        .userId(senderId)
                        .content(savedMessage.getContent())
                        .createdAt(createdAtIso)
                        .readByMe(false)
                        .readCount(0)
                        .settlementId(extractSettlementId(savedMessage))
                        .groupChatRoomId(chatRoom.getId().intValue())
                        .build();

                redisPublisher.publish(roomId, chatMessageDto);
                log.info("✅ [단계5 성공] Redis 메시지 발행 완료: messageId={}", savedMessage.getId());
            } catch (Exception e) {
                log.error("⚠️ [단계5 경고] Redis 발행 실패 (메시지는 DB에 저장됨): {}", e.getMessage());
                // Redis 발행 실패는 치명적이지 않으므로 계속 진행
            }

            // 6. 안 읽은 메시지 카운트 증가 (Redis 사용)
            log.debug("🔢 [단계6] 안 읽은 메시지 카운트 업데이트 중...");
            try {
                // 방의 모든 ACTIVE 멤버의 ID 조회
                log.debug("🔍 [단계6-1] 채팅방의 모든 멤버 ID 조회 중... roomId: {}", roomId);
                List<Long> memberIds = chatMemberRepository.findActiveMemberIdsByRoomId(roomId);
                log.debug("✅ [단계6-1] 멤버 조회 완료: memberCount={}", memberIds.size());

                // ChatRedisService를 통해 접속하지 않은 멤버의 카운트 증가
                log.debug("📈 [단계6-2] 접속하지 않은 멤버의 미읽음 카운트 증가 중...");
                chatRedisService.addUnreadMessageCount(roomId, senderId, memberIds);
                log.info("✅ [단계6 성공] 안 읽은 메시지 카운트 업데이트 완료");
            } catch (Exception e) {
                log.warn("⚠️ [단계6 경고] 안 읽은 메시지 카운트 업데이트 실패: {}", e.getMessage());
                // 카운트 업데이트 실패는 비즈니스 로직에 영향을 주지 않음
            }

            log.info("✅ [메시지 저장 및 발행 완료] roomId: {}, messageId: {}, sender: {}",
                    roomId, savedMessage.getId(), sender.getNickname());

            return toMessageResponse(savedMessage, senderId);

        } catch (Exception e) {
            log.error("❌ [메시지 저장 중 오류 발생] roomId: {}, senderId: {}, errorMsg: {}",
                    roomId, senderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 메시지 읽음 처리
     *
     * 마지막으로 읽은 메시지 ID를 저장
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long roomId, Long userId, UUID lastReadMessageId) {
        try {
            log.info("📖 [읽음 처리 시작] roomId: {}, userId: {}, lastReadMessageId: {}",
                    roomId, userId, lastReadMessageId);

            log.debug("🔍 [읽음처리-1] 채팅 멤버 조회 중... roomId: {}, userId: {}", roomId, userId);
            ChatMember member = chatMemberRepository.findMember(roomId, userId)
                    .orElseThrow(() -> {
                        log.error("❌ [읽음처리-1 실패] 채팅 멤버를 찾을 수 없음: roomId={}, userId={}", roomId, userId);
                        return new RuntimeException("Chat member not found");
                    });
            log.debug("✅ [읽음처리-1 성공] 멤버 조회됨: currentLastReadId={}", member.getLastReadMessageId());

            // 새로 읽은 메시지의 createdAt을 조회하여 비교
            ChatMessage newReadMessage = chatMessageRepository.findById(lastReadMessageId)
                    .orElseThrow(() -> {
                        log.error("❌ [읽음처리] 메시지를 찾을 수 없음: messageId={}", lastReadMessageId);
                        return new RuntimeException("Message not found: " + lastReadMessageId);
                    });

            // 이전에 읽은 메시지가 있으면 createdAt 기반으로 비교
            if (member.getLastReadMessageId() != null) {
                ChatMessage previousReadMessage = chatMessageRepository.findById(member.getLastReadMessageId())
                        .orElse(null);
                if (previousReadMessage != null &&
                        !previousReadMessage.getCreatedAt().isBefore(newReadMessage.getCreatedAt())) {
                    log.debug("⏭️ [읽음처리] 업데이트 스킵: 더 최신 메시지를 이미 읽음 - " +
                            "currentLastReadId: {}, newLastReadId: {}",
                            member.getLastReadMessageId(), lastReadMessageId);
                    return;
                }
            }

            log.debug("💾 [읽음처리-2] 마지막 읽은 메시지 ID 업데이트 중... {} -> {}",
                    member.getLastReadMessageId(), lastReadMessageId);
            member.setLastReadMessageId(lastReadMessageId);
            member.setLastReadAt(newReadMessage.getCreatedAt());

            log.debug("💾 [읽음처리-2] DB에 저장 중...");
            chatMemberRepository.save(member);
            log.info("✅ [읽음처리 완료] roomId: {}, userId: {}, lastReadMessageId: {}",
                    roomId, userId, lastReadMessageId);

        } catch (Exception e) {
            log.error("❌ [읽음 처리 중 오류 발생] roomId: {}, userId: {}, errorMsg: {}",
                    roomId, userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * DTO 변환
     */
    private MessageResponse toMessageResponse(ChatMessage message, Long requesterId) {
        ChatMember member = chatMemberRepository.findMember(
                message.getChatRoom().getId(),
                requesterId
        ).orElse(null);

        boolean readByMe = member != null &&
                member.getLastReadAt() != null &&
                !member.getLastReadAt().isBefore(message.getCreatedAt());

        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getNickname())
                .senderProfileImageUrl(message.getSender().getProfileImageUrl())
                .type(message.getType().toString())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .cardPayload(message.getCardPayload())
                .readCount(message.getReadCount())
                .createdAt(message.getCreatedAt())
                .readByMe(readByMe)
                .settlementId(extractSettlementId(message))
                .groupChatRoomId(message.getChatRoom().getId().intValue())
                .build();
    }

    /**
     * 메시지의 cardPayload에서 settlementId를 추출
     */
    private Integer extractSettlementId(ChatMessage message) {
        if (message.getCardPayload() == null || message.getCardPayload().isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(message.getCardPayload());
            if (node.has("settlementId") && !node.get("settlementId").isNull()) {
                return node.get("settlementId").asInt();
            }
        } catch (Exception e) {
            log.debug("cardPayload에서 settlementId 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 채팅방 과거 메시지 조회 (무한 스크롤용)
     *
     * 커서 기반 페이징을 사용하여 모바일 앱의 무한 스크롤 구현을 지원합니다.
     *
     * 처리 순서:
     * ① 요청한 사용자가 채팅방의 멤버인지 검증 (Authorization)
     * ② ChatMessageRepository.findMessagesByRoomIdBeforeCursorOrderByCreatedAtDesc() 호출
     * ③ cursor가 null이면 가장 최신 메시지부터 시작
     * ④ 조회된 메시지들을 MessageResponse로 변환 (6번 메시지 조회와 동일한 DTO)
     * ⑤ 클라이언트가 시간순으로 보기 쉽게 오름차순으로 정렬하여 반환
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param cursor 마지막으로 조회한 메시지의 생성 시간 (커서, null 가능)
     * @param size 조회할 메시지 개수 (기본 20)
     * @return 과거 메시지 리스트 (오름차순, 시간순)
     * @throws IllegalArgumentException 사용자가 채팅방의 멤버가 아닐 때
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getChatMessages(
            Long roomId,
            Long userId,
            java.time.LocalDateTime cursor,
            int size
    ) {
        try {
            log.info("📜 [과거 메시지 조회 시작] roomId: {}, userId: {}, cursor: {}, size: {}",
                    roomId, userId, cursor, size);

            // ① 요청한 사용자가 채팅방의 멤버인지 검증 (Authorization)
            log.debug("🔐 [단계1] 사용자 권한 검증 중... roomId: {}, userId: {}", roomId, userId);
            boolean isMember = chatMemberRepository.isActiveMember(roomId, userId);

            if (!isMember) {
                log.error("❌ [권한 검증 실패] userId {}는 roomId {} 멤버가 아님", userId, roomId);
                throw new IllegalArgumentException("이 채팅방에 접근 권한이 없습니다");
            }
            log.debug("✅ [단계1] 권한 검증 성공");

            // ② ChatMessageRepository 쿼리 호출
            log.debug("🔍 [단계2] 과거 메시지 조회 중... roomId: {}, cursor: {}, size: {}",
                    roomId, cursor, size);

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, size);

            List<ChatMessage> messages = chatMessageRepository.findMessagesByRoomIdBeforeCursorOrderByCreatedAtDesc(
                    roomId,
                    cursor,
                    pageable
            );
            log.info("✅ [단계2] 메시지 조회 완료: messageCount={}", messages.size());

            // ③ 조회된 메시지들을 MessageResponse로 변환 (6번 API와 동일한 DTO)
            log.debug("🔄 [단계3] MessageResponse로 변환 중...");
            List<MessageResponse> messageResponses = messages.stream()
                    .map(msg -> {
                        boolean readByMe = msg.getSender().getId().equals(userId)
                                || (msg.getReadCount() != null && msg.getReadCount() > 0);

                        return MessageResponse.builder()
                                .id(msg.getId())
                                .roomId(msg.getChatRoom().getId())
                                .senderId(msg.getSender() != null ? msg.getSender().getId() : null)
                                .userId(msg.getSender() != null ? msg.getSender().getId() : null)
                                .senderName(msg.getSender() != null ? msg.getSender().getNickname() : "알 수 없음")
                                .nickname(msg.getSender() != null ? msg.getSender().getNickname() : "알 수 없음")
                                .senderProfileImageUrl(msg.getSender() != null ? msg.getSender().getProfileImageUrl() : null)
                                .profileImage(msg.getSender() != null ? msg.getSender().getProfileImageUrl() : null)
                                .type(msg.getType().toString())
                                .content(msg.getContent())
                                .imageUrl(msg.getImageUrl())
                                .cardPayload(msg.getCardPayload())
                                .readCount(msg.getReadCount())
                                .createdAt(msg.getCreatedAt())
                                .readByMe(readByMe)
                                .settlementId(extractSettlementId(msg))
                                .groupChatRoomId(msg.getChatRoom().getId().intValue())
                                .build();
                    })
                    .toList();

            // ④ 클라이언트가 시간순으로 보기 쉽게 오름차순으로 정렬
            log.debug("🔄 [단계4] 메시지를 시간순(오름차순)으로 정렬 중...");
            List<MessageResponse> sortedMessages = messageResponses.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .toList();

            log.info("✅ [과거 메시지 조회 완료] roomId: {}, messageCount: {}", roomId, sortedMessages.size());
            return sortedMessages;

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [과거 메시지 조회 실패] 유효하지 않은 요청 - roomId: {}, userId: {}", roomId, userId);
            log.warn("   - errorMsg: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ [과거 메시지 조회 실패] roomId: {}, userId: {}, error: {}",
                    roomId, userId, e.getMessage(), e);
            throw new RuntimeException("과거 메시지 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 시스템 메시지 저장 및 발행 (ENTER / LEAVE)
     *
     * 멤버십 검증을 건너뜁니다.
     * - ENTER: 방에 추가된 직후 호출 (이미 멤버 상태)
     * - LEAVE: 방에서 제거된 직후 호출 (더 이상 멤버가 아닌 상태)
     * 두 경우 모두 일반 saveMessage()의 isMember 체크를 통과시킬 수 없으므로
     * 별도 메서드로 분리합니다.
     *
     * @param roomId    채팅방 ID
     * @param user      메시지 발신자 (입장/퇴장 당사자)
     * @param type      ENTER 또는 LEAVE
     * @param content   표시할 시스템 메시지 텍스트 (e.g. "홍길동님이 입장했습니다.")
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishSystemMessage(Long roomId, User user, ChatMessageType type, String content) {
        try {
            log.info("📢 [시스템 메시지 발행] roomId: {}, userId: {}, type: {}", roomId, user.getId(), type);

            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

            // 메시지 저장
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(user)
                    .type(type)
                    .content(content)
                    .readCount(0)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(message);

            // 채팅방 메타데이터 업데이트
            chatRoom.setLastMessageAt(savedMessage.getCreatedAt());
            chatRoom.setLastMessagePreview(content);
            chatRoom.setLastMessageSenderId(user.getId());
            chatRoomRepository.save(chatRoom);

            // Redis Pub/Sub 발행
            try {
                String createdAtIso = savedMessage.getCreatedAt()
                        .atZone(java.time.ZoneId.of("Asia/Seoul"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

                ChatMessageDto dto = ChatMessageDto.builder()
                        .type(type)
                        .roomId(roomId)
                        .senderId(user.getId())
                        .senderName(user.getNickname())
                        .nickname(user.getNickname())
                        .profileImage(user.getProfileImageUrl())
                        .senderProfileImageUrl(user.getProfileImageUrl())
                        .userId(user.getId())
                        .message(content)
                        .content(content)
                        .messageId(savedMessage.getId())
                        .id(savedMessage.getId().toString())
                        .createdAt(createdAtIso)
                        .timestamp(System.currentTimeMillis())
                        .readByMe(false)
                        .readCount(0)
                        .groupChatRoomId(roomId.intValue())
                        .build();

                redisPublisher.publish(roomId, dto);
                log.info("✅ [시스템 메시지 발행 완료] roomId: {}, type: {}, messageId: {}",
                        roomId, type, savedMessage.getId());
            } catch (Exception e) {
                log.warn("⚠️ [시스템 메시지 Redis 발행 실패] DB 저장은 완료됨: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ [시스템 메시지 발행 오류] roomId: {}, userId: {}, type: {}, error: {}",
                    roomId, user.getId(), type, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 미리보기 텍스트 생성 (너무 길면 잘라냠)
     */
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
