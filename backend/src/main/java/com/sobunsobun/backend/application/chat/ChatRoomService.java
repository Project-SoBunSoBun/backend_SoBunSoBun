package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.application.notification.NotificationService;
import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.SavedPost;
import com.sobunsobun.backend.domain.SettlementStatus;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.domain.chat.ChatRoomType;
import com.sobunsobun.backend.domain.chat.ChatMemberStatus;
import com.sobunsobun.backend.dto.chat.ChatRoomDetailResponse;
import com.sobunsobun.backend.dto.chat.ChatRoomListResponseDto;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomResponse;
import com.sobunsobun.backend.dto.chat.LastMessageDto;
import com.sobunsobun.backend.infrastructure.redis.ChatRedisService;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.MannerReviewRepository;
import com.sobunsobun.backend.repository.SavedPostRepository;
import com.sobunsobun.backend.repository.SettlementRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.exception.UserException;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 채팅방 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GroupPostRepository groupPostRepository;
    private final ChatRedisService chatRedisService;
    private final ChatMessageService chatMessageService;
    private final SettlementRepository settlementRepository;
    private final MannerReviewRepository mannerReviewRepository;
    private final SavedPostRepository savedPostRepository;
    private final NotificationService notificationService;

    /**
     * 채팅방 목록 조회
     *
     * 인증된 사용자의 모든 채팅방 목록을 반환합니다.
     * 각 채팅방의 마지막 메시지와 안 읽은 메시지 개수를 포함합니다.
     *
     * 처리 순서:
     * ① ChatMemberRepository를 통해 userId가 속한 모든 채팅방 조회
     * ② 각 채팅방의 가장 최근 메시지를 ChatMessageRepository에서 조회
     * ③ ChatRedisService.getUnreadCount()로 안 읽은 카운트 조회
     * ④ 1:1 채팅인 경우 상대방 이름으로 roomName 설정
     * ⑤ lastMessageTime 기준 내림차순 정렬
     *
     * @param userId 사용자 ID
     * @return 채팅방 목록 (최신순 정렬)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListResponseDto> getChatRoomList(Long userId) {
        try {
            log.info(" [채팅방 목록 조회 시작] userId: {}", userId);

            // ① ChatMemberRepository를 통해 userId가 속한 모든 채팅방 조회
            log.debug(" [단계1] 사용자가 속한 채팅방 조회 중... userId: {}", userId);
            List<ChatMember> chatMembers = chatMemberRepository.findChatRoomsByUserId(userId);
            log.debug(" [단계1] 채팅방 조회 완료: roomCount={}", chatMembers.size());

            if (chatMembers.isEmpty()) {
                log.info("ℹ 사용자가 속한 채팅방이 없습니다. userId: {}", userId);
                return List.of();
            }

            // ② 각 채팅방을 순회하면서 DTO로 변환
            log.debug(" [단계2] 각 채팅방을 DTO로 변환 중...");
            List<ChatRoomListResponseDto> chatRoomList = chatMembers.stream()
                    .map(chatMember -> {
                        ChatRoom chatRoom = chatMember.getChatRoom();
                        Long roomId = chatRoom.getId();

                        try {
                            // ② 해당 방의 가장 최근 메시지 조회
                            log.debug("   [방{}] 최근 메시지 조회 중...", roomId);
                            Optional<ChatMessage> latestMessage = chatMessageRepository.findLatestMessageByRoomId(roomId);

                            // ③ ChatRedisService.getUnreadCount() 호출
                            log.debug("   [방{}] 안 읽은 메시지 개수 조회 중...", roomId);
                            Long unreadCount = chatRedisService.getUnreadCount(roomId, userId);

                            // ④ 1:1 채팅인 경우 상대방 이름/프로필로 설정, GROUP은 방 이름 사용
                            String roomName = chatRoom.getName();
                            String profileImageUrl = null;
                            if (chatRoom.getRoomType() == ChatRoomType.ONE_TO_ONE) {
                                log.debug("   [방{}] 1:1 채팅 - 상대방 정보로 설정 중...", roomId);
                                roomName = getPrivateChatRoomName(chatRoom, userId);
                                profileImageUrl = getPrivateChatProfileImage(chatRoom, userId);
                            }

                            log.debug("   [방{}] DTO 변환 완료: roomName={}, unreadCount={}",
                                    roomId, roomName, unreadCount);

                            return ChatRoomListResponseDto.builder()
                                    .roomId(roomId)
                                    .roomName(roomName)
                                    .profileImageUrl(profileImageUrl)
                                    .roomType(chatRoom.getRoomType().toString())
                                    .memberCount(chatRoom.getMembers().size())
                                    .lastMessage(latestMessage.map(LastMessageDto::from).orElse(null))
                                    .unreadCount(unreadCount)
                                    .groupPostId(chatRoom.getGroupPost() != null ? chatRoom.getGroupPost().getId() : null)
                                    .build();

                        } catch (Exception e) {
                            log.error(" [방{}] DTO 변환 중 오류 발생: {}", roomId, e.getMessage());
                            // 하나의 방 변환 실패가 전체를 실패시키지 않도록 에러만 로깅
                            throw new RuntimeException("방 " + roomId + " 정보 조회 실패: " + e.getMessage(), e);
                        }
                    })
                    .collect(Collectors.toList());

            // ⑤ lastMessageTime 기준 최신순(내림차순) 정렬
            log.debug(" [단계3] lastMessageTime 기준 정렬 중...");
            List<ChatRoomListResponseDto> sortedList = chatRoomList.stream()
                    .sorted((a, b) -> {
                        String aTime = a.getLastMessage() != null ? a.getLastMessage().getCreatedAt() : null;
                        String bTime = b.getLastMessage() != null ? b.getLastMessage().getCreatedAt() : null;
                        if (aTime == null && bTime == null) return 0;
                        if (aTime == null) return 1;
                        if (bTime == null) return -1;
                        return bTime.compareTo(aTime); // 내림차순: 최신이 먼저
                    })
                    .collect(Collectors.toList());

            log.info(" [채팅방 목록 조회 완료] userId: {}, roomCount: {}", userId, sortedList.size());
            return sortedList;

        } catch (Exception e) {
            log.error(" [채팅방 목록 조회 실패] userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("채팅방 목록 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 1:1 채팅방의 상대방 이름 조회
     *
     * 1:1 채팅방의 두 멤버 중 userId가 아닌 다른 사용자의 이름을 반환합니다.
     *
     * @param chatRoom 채팅방 엔티티
     * @param userId 현재 사용자 ID
     * @return 상대방 닉네임 (없으면 채팅방 이름 반환)
     */
    private String getPrivateChatRoomName(ChatRoom chatRoom, Long userId) {
        try {
            // 채팅방의 멤버 중 현재 사용자가 아닌 사용자의 이름을 찾음
            return chatRoom.getMembers().stream()
                    .filter(member -> !member.getUser().getId().equals(userId))
                    .findFirst()
                    .map(member -> member.getUser().getNickname())
                    .orElse(chatRoom.getName()); // 상대방을 찾지 못하면 채팅방 이름 반환
        } catch (Exception e) {
            log.warn(" 상대방 이름 조회 실패, 기본값 사용: {}", chatRoom.getName());
            return chatRoom.getName();
        }
    }

    /**
     * 1:1 채팅방에서 상대방 프로필 이미지 조회
     *
     * @param chatRoom 채팅방 엔티티
     * @param userId 현재 사용자 ID
     * @return 상대방 프로필 이미지 URL (없으면 null)
     */
    private String getPrivateChatProfileImage(ChatRoom chatRoom, Long userId) {
        try {
            return chatRoom.getMembers().stream()
                    .filter(member -> !member.getUser().getId().equals(userId))
                    .findFirst()
                    .map(member -> member.getUser().getProfileImageUrl())
                    .orElse(null);
        } catch (Exception e) {
            log.warn(" 상대방 프로필 이미지 조회 실패: roomId={}", chatRoom.getId());
            return null;
        }
    }

    /**
     * 개인 채팅방 생성 또는 조회
     */
    public ChatRoom getOrCreatePrivateChatRoom(Long userId1, Long userId2, Long groupPostId) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info(" [개인 채팅방 생성/조회 시작] userId1: {}, userId2: {}, groupPostId: {}", userId1, userId2, groupPostId);

            // 기존 채팅방 조회
            log.debug(" [단계1] 기존 개인 채팅방 조회 중...");
            Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoom(userId1, userId2, groupPostId);
            if (existingRoom.isPresent()) {
                log.info(" [단계1 완료] 기존 개인 채팅방 발견 - roomId: {}", existingRoom.get().getId());
                log.info("═════════════════════════════════════════════════════════════");
                return existingRoom.get();
            }
            log.info("ℹ [단계1 완료] 기존 채팅방 없음 - 새로 생성 필요");

            // 새 채팅방 생성
            log.debug(" [단계2] User1 조회 중... userId: {}", userId1);
            User user1 = userRepository.findById(userId1)
                    .orElseThrow(() -> {
                        log.error(" [단계2 실패] User1을 찾을 수 없음: userId={}", userId1);
                        return new IllegalArgumentException("존재하지 않는 사용자입니다 (userId: " + userId1 + ")");
                    });
            log.info(" [단계2 완료] User1 조회됨: {}", user1.getNickname());

            log.debug(" [단계3] User2 조회 중... userId: {}", userId2);
            User user2 = userRepository.findById(userId2)
                    .orElseThrow(() -> {
                        log.error(" [단계3 실패] User2를 찾을 수 없음: userId={}", userId2);
                        return new IllegalArgumentException("존재하지 않는 사용자입니다 (userId: " + userId2 + ")");
                    });
            log.info(" [단계3 완료] User2 조회됨: {}", user2.getNickname());

            GroupPost groupPost = null;
            if (groupPostId != null) {
                log.debug(" [단계3-1] 게시글 조회 중... groupPostId: {}", groupPostId);
                groupPost = groupPostRepository.findById(groupPostId)
                        .orElseThrow(() -> {
                            log.error(" [단계3-1 실패] 게시글을 찾을 수 없음: groupPostId={}", groupPostId);
                            return new IllegalArgumentException("존재하지 않는 게시글입니다 (groupPostId: " + groupPostId + ")");
                        });
                log.info(" [단계3-1 완료] 게시글 조회됨: {}", groupPost.getTitle());
            }

            log.debug(" [단계4] ChatRoom 엔티티 생성 중...");
            ChatRoom chatRoom = ChatRoom.builder()
                    .name(user2.getNickname())  // 개인 채팅방은 상대방 이름으로 표시
                    .roomType(ChatRoomType.ONE_TO_ONE)
                    .owner(user2)
                    .groupPost(groupPost)
                    .messageCount(0L)
                    .build();
            log.info(" [단계4 완료] ChatRoom 엔티티 생성됨");

            log.debug(" [단계5] ChatRoom DB 저장 중...");
            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(chatRoom);
            log.info(" [단계5 완료] ChatRoom DB 저장됨 - roomId: {}", savedRoom.getId());

            // 두 사용자를 멤버로 추가
            log.debug(" [단계6] ChatMember 엔티티 생성 중...");
            ChatMember member1 = savedRoom.addMember(user1);
            ChatMember member2 = savedRoom.addMember(user2);
            log.info(" [단계6 완료] ChatMember 엔티티 생성됨");

            log.debug(" [단계7] ChatMember DB 저장 중... member count: 2");
            // 멤버 저장
            chatMemberRepository.saveAndFlush(member1);
            chatMemberRepository.saveAndFlush(member2);
            log.info(" [단계7 완료] ChatMember DB 저장됨");

            // WebSocket으로 멤버 양쪽에 새 채팅방 알림 발송
            chatMessageService.broadcastNewRoomNotification(savedRoom);

            log.info(" [개인 채팅방 생성 완료] roomId: {}, owner: {}, member: {} <-> {}",
                    savedRoom.getId(), user1.getNickname(), user1.getNickname(), user2.getNickname());
            log.info("═════════════════════════════════════════════════════════════");

            return savedRoom;

        } catch (IllegalArgumentException e) {
            log.warn(" [개인 채팅방 생성 실패] 유효하지 않은 요청 - userId1: {}, userId2: {}", userId1, userId2);
            log.warn("   - errorMsg: {}", e.getMessage());
            throw e;  // 그대로 전파하여 컨트롤러에서 처리
        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error(" [개인 채팅방 생성 실패] 예외 발생", e);
            log.error("   - userId1: {}, userId2: {}", userId1, userId2);
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");
            throw new RuntimeException("개인 채팅방 생성 실패: " + e.getMessage(), e);
        }
    }


    /**
     * 채팅방에 멤버 추가
     */
    public void addMember(Long roomId, Long userId) {
        log.info(" 멤버 추가 - roomId: {}, userId: {}", roomId, userId);

        try {
            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // 이미 멤버인지 확인
            if (chatRoom.isMember(userId)) {
                log.warn(" 이미 멤버임 - roomId: {}, userId: {}", roomId, userId);
                return;
            }

            ChatMember newMember = chatRoom.addMember(user);
            // 명시적 저장
            log.info(" ChatMember 저장 중...");
            chatMemberRepository.saveAndFlush(newMember);

            log.info(" 멤버 추가 완료 - roomId: {}, userId: {}, memberId: {}",
                    roomId, userId, newMember.getId());

        } catch (Exception e) {
            log.error(" 멤버 추가 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new RuntimeException("멤버 추가 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 채팅방을 응답 DTO로 변환 (unreadCount 포함)
     */
    public ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, Long userId) {
        // 안 읽은 메시지 개수 조회
        long unreadCount = chatMemberRepository.countUnreadMessages(chatRoom.getId(), userId);

        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .roomType(chatRoom.getRoomType().toString())
                .ownerId(chatRoom.getOwner() != null ? chatRoom.getOwner().getId() : null)
                .memberCount(chatRoom.getMembers().size())
                .unreadCount(unreadCount)
                .lastMessagePreview(chatRoom.getLastMessagePreview())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .build();
    }

    /**
     * 1:1 채팅방 생성 또는 기존 방 조회
     *
     * 두 사용자 간의 1:1 채팅방이 이미 있으면 기존 방을 반환하고,
     * 없으면 새로운 ONE_TO_ONE 타입의 채팅방을 생성합니다.
     *
     * 처리 순서:
     * ① ChatMemberRepository를 통해 기존 1:1 채팅방 확인
     * ② 이미 방이 있다면 기존 roomId 반환
     * ③ 방이 없다면 새로운 ChatRoom(type=ONE_TO_ONE) 생성
     * ④ 두 명의 ChatMember 생성 및 저장
     * ⑤ 새로운 roomId 반환
     *
     * @param myId 현재 사용자 ID
     * @param targetId 상대방 사용자 ID
     * @return 채팅방 정보를 포함한 응답 DTO
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    @Transactional
    public com.sobunsobun.backend.dto.chat.CreateOneToOneRoomResponse createOrGetOneToOneRoom(
            Long myId,
            Long targetId,
            Long groupPostId
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info(" [1:1 채팅방 생성/조회 시작] myId: {}, targetId: {}, groupPostId: {}", myId, targetId, groupPostId);

            // 자신과의 채팅 방지
            if (myId.equals(targetId)) {
                log.error(" 자신과의 1:1 채팅은 불가능합니다");
                throw new ChatException(ErrorCode.CHAT_SELF_CHAT_NOT_ALLOWED);
            }

            // ① ChatMemberRepository를 통해 기존 1:1 채팅방 확인
            log.debug(" [단계1] 기존 1:1 채팅방 확인 중... myId: {}, targetId: {}, groupPostId: {}", myId, targetId, groupPostId);
            var existingRoom = chatMemberRepository.findOneToOneChatRoom(myId, targetId, groupPostId);

            if (existingRoom.isPresent()) {
                log.info(" [단계1] 기존 1:1 채팅방 발견 - roomId: {}", existingRoom.get().getId());

                ChatRoom room = existingRoom.get();
                User otherUser = room.getMembers().stream()
                        .filter(m -> !m.getUser().getId().equals(myId))
                        .map(ChatMember::getUser)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("상대방 정보를 찾을 수 없습니다"));

                log.info("═════════════════════════════════════════════════════════════");
                return com.sobunsobun.backend.dto.chat.CreateOneToOneRoomResponse.builder()
                        .roomId(room.getId())
                        .otherUserName(otherUser.getNickname())
                        .otherUserProfileImageUrl(otherUser.getProfileImageUrl())
                        .isNewRoom(false)
                        .build();
            }

            log.info("ℹ [단계1] 기존 채팅방 없음 - 새로 생성 필요");

            // ② 사용자 조회
            log.debug(" [단계2] 사용자 조회 중... myId: {}", myId);
            User myUser = userRepository.findById(myId)
                    .orElseThrow(() -> {
                        log.error(" [단계2 실패] 현재 사용자를 찾을 수 없음: myId={}", myId);
                        return UserException.notFound();
                    });
            log.info(" [단계2] 현재 사용자 조회됨: {}", myUser.getNickname());

            log.debug(" [단계3] 상대방 사용자 조회 중... targetId: {}", targetId);
            User targetUser = userRepository.findById(targetId)
                    .orElseThrow(() -> {
                        log.error(" [단계3 실패] 상대방 사용자를 찾을 수 없음: targetId={}", targetId);
                        return UserException.notFound();
                    });
            log.info(" [단계3] 상대방 사용자 조회됨: {}", targetUser.getNickname());

            GroupPost groupPost = null;
            if (groupPostId != null) {
                log.debug(" [단계3-1] 게시글 조회 중... groupPostId: {}", groupPostId);
                groupPost = groupPostRepository.findById(groupPostId)
                        .orElseThrow(() -> {
                            log.error(" [단계3-1 실패] 게시글을 찾을 수 없음: groupPostId={}", groupPostId);
                            return new IllegalArgumentException("존재하지 않는 게시글입니다 (groupPostId: " + groupPostId + ")");
                        });
                log.info(" [단계3-1 완료] 게시글 조회됨: {}", groupPost.getTitle());
            }

            // ③ 새로운 ChatRoom 생성 (ONE_TO_ONE 타입)
            log.debug(" [단계4] ChatRoom 엔티티 생성 중...");
            ChatRoom newRoom = ChatRoom.builder()
                    .name(targetUser.getNickname())  // 1:1 채팅방은 상대방 이름으로 표시
                    .roomType(ChatRoomType.ONE_TO_ONE)
                    .owner(myUser)
                    .groupPost(groupPost)
                    .messageCount(0L)
                    .build();
            log.info(" [단계4] ChatRoom 엔티티 생성됨");

            log.debug(" [단계4] ChatRoom DB 저장 중...");
            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(newRoom);
            log.info(" [단계4 완료] ChatRoom DB 저장됨 - roomId: {}", savedRoom.getId());

            // ④ 두 명의 ChatMember 생성 및 저장
            log.debug(" [단계5] ChatMember 엔티티 생성 중...");
            ChatMember member1 = savedRoom.addMember(myUser);
            ChatMember member2 = savedRoom.addMember(targetUser);
            log.info(" [단계5] ChatMember 엔티티 생성됨");

            log.debug(" [단계5] ChatMember DB 저장 중...");
            chatMemberRepository.saveAndFlush(member1);
            chatMemberRepository.saveAndFlush(member2);
            log.info(" [단계5 완료] ChatMember DB 저장됨");

            // WebSocket으로 멤버 양쪽에 새 채팅방 알림 발송
            chatMessageService.broadcastNewRoomNotification(savedRoom);

            log.info(" [1:1 채팅방 생성 완료] roomId: {}, members: {} <-> {}",
                    savedRoom.getId(), myUser.getNickname(), targetUser.getNickname());
            log.info("═════════════════════════════════════════════════════════════");

            return com.sobunsobun.backend.dto.chat.CreateOneToOneRoomResponse.builder()
                    .roomId(savedRoom.getId())
                    .otherUserName(targetUser.getNickname())
                    .otherUserProfileImageUrl(targetUser.getProfileImageUrl())
                    .isNewRoom(true)
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn(" [1:1 채팅방 생성 실패] 유효하지 않은 요청 - myId: {}, targetId: {}", myId, targetId);
            log.warn("   - errorMsg: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error(" [1:1 채팅방 생성 실패] 예외 발생", e);
            log.error("   - myId: {}, targetId: {}", myId, targetId);
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");
            throw new RuntimeException("1:1 채팅방 생성 실패: " + e.getMessage(), e);
        }
    }

    // ====== 단체 채팅방 관련 메서드 ======

    /**
     * 단체 채팅방 생성 또는 기존 방 조회
     *
     * 공동구매 게시글에 연결된 단체 채팅방을 생성하거나,
     * 이미 존재하면 기존 채팅방을 반환합니다 (getOrCreate 패턴).
     *
     * 처리 순서:
     * ① groupPostId로 기존 단체 채팅방이 있는지 확인
     * ② 이미 존재하면 기존 방 정보 반환
     * ③ 없으면 새 ChatRoom(type=GROUP) 생성
     * ④ 요청자(owner)를 첫 번째 멤버로 추가
     * ⑤ memberIds에 포함된 사용자들을 멤버로 추가
     *
     * @param userId     요청 사용자 ID (방장)
     * @param roomName   채팅방 이름
     * @param groupPostId 공동구매 게시글 ID
     * @param memberIds  초대할 사용자 ID 목록 (선택, null 가능)
     * @return 채팅방 생성/조회 응답 DTO
     * @throws IllegalArgumentException 게시글이 존재하지 않을 때
     */
    @Transactional
    public CreateChatRoomResponse createOrGetGroupChatRoom(
            Long userId,
            String roomName,
            Long groupPostId,
            java.util.List<Long> memberIds
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info(" [단체 채팅방 생성/조회 시작] userId: {}, groupPostId: {}, roomName: {}",
                    userId, groupPostId, roomName);

            // ① 공동구매 게시글 존재 확인
            log.debug(" [단계1] 공동구매 게시글 조회 중... groupPostId: {}", groupPostId);
            GroupPost groupPost = groupPostRepository.findById(groupPostId)
                    .orElseThrow(() -> {
                        log.error(" [단계1 실패] 게시글을 찾을 수 없음: groupPostId={}", groupPostId);
                        return new IllegalArgumentException("존재하지 않는 게시글입니다 (groupPostId: " + groupPostId + ")");
                    });
            log.info(" [단계1 완료] 게시글 조회됨: {}", groupPost.getTitle());

            // ② 기존 단체 채팅방 확인
            log.debug(" [단계2] 기존 단체 채팅방 확인 중... groupPostId: {}", groupPostId);
            Optional<ChatRoom> existingRoom = chatRoomRepository.findByGroupPostId(groupPostId);

            if (existingRoom.isPresent()) {
                ChatRoom room = existingRoom.get();
                log.info(" [단계2] 기존 단체 채팅방 발견 - roomId: {}", room.getId());

                // 요청자가 멤버가 아니면 멤버로 추가
                if (!room.isMember(userId)) {
                    log.info("ℹ 요청자가 멤버가 아닙니다. 멤버로 추가합니다. userId: {}", userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다 (userId: " + userId + ")"));
                    ChatMember newMember = room.addMember(user);
                    chatMemberRepository.saveAndFlush(newMember);

                    // ENTER 시스템 메시지 발행
                    chatMessageService.publishSystemMessage(
                            room.getId(),
                            user,
                            ChatMessageType.ENTER,
                            user.getNickname() + "님이 입장했습니다."
                    );
                }

                int memberCount = (int) chatMemberRepository.findActiveMembersByRoomId(room.getId()).size();

                log.info("═════════════════════════════════════════════════════════════");
                return CreateChatRoomResponse.builder()
                        .roomId(room.getId())
                        .roomName(room.getName())
                        .roomType(room.getRoomType().toString())
                        .groupPostId(groupPostId)
                        .memberCount(memberCount)
                        .isNewRoom(false)
                        .message(" 기존 단체 채팅방 조회 성공")
                        .build();
            }

            log.info("ℹ [단계2] 기존 채팅방 없음 - 새로 생성 필요");

            // ③ 요청 사용자(방장) 조회
            log.debug(" [단계3] 요청 사용자 조회 중... userId: {}", userId);
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error(" [단계3 실패] 사용자를 찾을 수 없음: userId={}", userId);
                        return new IllegalArgumentException("존재하지 않는 사용자입니다 (userId: " + userId + ")");
                    });
            log.info(" [단계3 완료] 사용자 조회됨: {}", owner.getNickname());

            // ④ 채팅방 이름 결정: 요청값이 없으면 게시글 제목 사용
            String finalRoomName = (roomName != null && !roomName.isBlank()) ? roomName : groupPost.getTitle();

            // ⑤ 새로운 ChatRoom 생성 (GROUP 타입)
            log.debug(" [단계4] ChatRoom 엔티티 생성 중...");
            ChatRoom chatRoom = ChatRoom.builder()
                    .name(finalRoomName)
                    .roomType(ChatRoomType.GROUP)
                    .owner(owner)
                    .groupPost(groupPost)
                    .messageCount(0L)
                    .build();
            log.info(" [단계4 완료] ChatRoom 엔티티 생성됨");

            log.debug(" [단계5] ChatRoom DB 저장 중...");
            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(chatRoom);
            log.info(" [단계5 완료] ChatRoom DB 저장됨 - roomId: {}", savedRoom.getId());

            // ⑥ 방장을 첫 번째 멤버로 추가
            log.debug(" [단계6] 방장을 첫 번째 멤버로 추가 중...");
            ChatMember ownerMember = savedRoom.addMember(owner);
            chatMemberRepository.saveAndFlush(ownerMember);
            log.info(" [단계6 완료] 방장 멤버 추가됨");

            // ⑦ 추가 멤버 초대
            int addedMemberCount = 1; // 방장 포함
            if (memberIds != null && !memberIds.isEmpty()) {
                log.debug(" [단계7] 추가 멤버 초대 중... memberIds: {}", memberIds);
                for (Long memberId : memberIds) {
                    // 방장은 이미 추가됨
                    if (memberId.equals(userId)) {
                        continue;
                    }
                    try {
                        User memberUser = userRepository.findById(memberId).orElse(null);
                        if (memberUser == null) {
                            log.warn(" 존재하지 않는 사용자 건너뜀: userId={}", memberId);
                            continue;
                        }
                        ChatMember newMember = savedRoom.addMember(memberUser);
                        chatMemberRepository.saveAndFlush(newMember);
                        addedMemberCount++;
                        log.debug("   멤버 추가됨: userId={}, nickname={}", memberId, memberUser.getNickname());
                    } catch (Exception e) {
                        log.warn(" 멤버 추가 실패 (건너뜀): userId={}, error={}", memberId, e.getMessage());
                    }
                }
                log.info(" [단계7 완료] 총 {} 명 멤버 추가됨", addedMemberCount);
            }

            // WebSocket으로 모든 멤버에게 새 채팅방 알림 발송
            chatMessageService.broadcastNewRoomNotification(savedRoom);

            log.info(" [단체 채팅방 생성 완료] roomId: {}, roomName: {}, memberCount: {}, groupPostId: {}",
                    savedRoom.getId(), finalRoomName, addedMemberCount, groupPostId);
            log.info("═════════════════════════════════════════════════════════════");

            return CreateChatRoomResponse.builder()
                    .roomId(savedRoom.getId())
                    .roomName(finalRoomName)
                    .roomType(ChatRoomType.GROUP.toString())
                    .groupPostId(groupPostId)
                    .memberCount(addedMemberCount)
                    .isNewRoom(true)
                    .message(" 단체 채팅방 생성 성공")
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn(" [단체 채팅방 생성 실패] 유효하지 않은 요청");
            log.warn("   - userId: {}, groupPostId: {}", userId, groupPostId);
            log.warn("   - errorMsg: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error(" [단체 채팅방 생성 실패] 예외 발생", e);
            log.error("   - userId: {}, groupPostId: {}", userId, groupPostId);
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");
            throw new RuntimeException("단체 채팅방 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 단체 채팅방에 멤버 추가 (초대)
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID (초대하는 사람)
     * @param targetUserId 초대할 사용자 ID
     * @throws IllegalArgumentException 채팅방이 단체 채팅이 아니거나, 권한이 없을 때
     */
    @Transactional
    public void addMemberToGroupChat(Long roomId, Long userId, Long targetUserId) {
        try {
            log.info(" [단체 채팅 멤버 초대] roomId: {}, inviter: {}, target: {}", roomId, userId, targetUserId);

            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 단체 채팅방인지 확인
            if (chatRoom.getRoomType() != ChatRoomType.GROUP) {
                throw new ChatException(ErrorCode.CHAT_INVALID_ROOM_TYPE, "단체 채팅방에서만 멤버를 초대할 수 있습니다.");
            }

            // 요청자가 멤버인지 확인
            if (!chatRoom.isMember(userId)) {
                throw new ChatException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }

            // 이미 멤버인지 확인
            if (chatRoom.isMember(targetUserId)) {
                log.warn(" 이미 멤버임 - roomId: {}, targetUserId: {}", roomId, targetUserId);
                return;
            }

            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(UserException::notFound);

            ChatMember newMember = chatRoom.addMember(targetUser);
            chatMemberRepository.saveAndFlush(newMember);

            // ENTER 시스템 메시지 발행
            chatMessageService.publishSystemMessage(
                    roomId,
                    targetUser,
                    ChatMessageType.ENTER,
                    targetUser.getNickname() + "님이 입장했습니다."
            );

            log.info(" [단체 채팅 멤버 초대 완료] roomId: {}, targetUser: {}", roomId, targetUser.getNickname());

        } catch (ChatException | UserException e) {
            throw e;
        } catch (Exception e) {
            log.error(" 단체 채팅 멤버 초대 실패 - roomId: {}, targetUserId: {}", roomId, targetUserId, e);
            throw new RuntimeException("멤버 초대 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 채팅방 나가기 (1:1 및 단체 채팅 모두 지원)
     * - 단체 채팅: 정산 진행 중(PENDING)이면 퇴장 불가
     * - 1:1 채팅: 제한 없이 퇴장 가능
     *
     * @param roomId 채팅방 ID
     * @param userId 나가는 사용자 ID
     */
    @Transactional
    public void leaveChatRoom(Long roomId, Long userId) {
        try {
            log.info(" [채팅 퇴장] roomId: {}, userId: {}", roomId, userId);

            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            if (!chatRoom.isMember(userId)) {
                throw new ChatException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
            }

            // 단체 채팅: 정산 진행 중(PENDING) 퇴장 차단
            if (chatRoom.getRoomType() == ChatRoomType.GROUP && chatRoom.getGroupPost() != null) {
                boolean settlementPending = settlementRepository.existsByGroupPostIdAndStatus(
                        chatRoom.getGroupPost().getId(), SettlementStatus.PENDING);
                if (settlementPending) {
                    log.warn("[퇴장 차단] 정산 진행 중 - roomId: {}, userId: {}", roomId, userId);
                    throw new ChatException(ErrorCode.CHAT_SETTLEMENT_IN_PROGRESS);
                }
            }

            // 퇴장 전에 유저 정보 미리 조회 (제거 후에는 멤버 조회 불가)
            User leavingUser = userRepository.findById(userId)
                    .orElseThrow(UserException::notFound);

            chatRoom.removeMember(userId);
            chatRoomRepository.save(chatRoom);

            // 단체 채팅만 LEAVE 시스템 메시지 발행
            if (chatRoom.getRoomType() == ChatRoomType.GROUP) {
                chatMessageService.publishSystemMessage(
                        roomId,
                        leavingUser,
                        ChatMessageType.LEAVE,
                        leavingUser.getNickname() + "님이 퇴장했습니다."
                );

                // 저장한 글 자리가 빌 때 알림
                GroupPost groupPost = chatRoom.getGroupPost();
                if (groupPost != null) {
                    notifySavedPostUsers(groupPost.getId(), userId);
                }
            }

            log.info(" [채팅 퇴장 완료] roomId: {}, userId: {}", roomId, userId);

        } catch (ChatException | UserException e) {
            throw e;
        } catch (Exception e) {
            log.error(" 채팅 퇴장 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new RuntimeException("채팅방 퇴장 실패: " + e.getMessage(), e);
        }
    }

    // ====== 채팅방 상세 정보 조회 ======

    /**
     * 채팅방 상세 정보 조회
     *
     * 개인(ONE_TO_ONE) / 단체(GROUP) 채팅방 모두 지원합니다.
     * - 개인: 상대방 유저 정보 포함
     * - 단체: 멤버 목록 + 연결된 게시글 정보 포함
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @return 채팅방 상세 정보 DTO
     */
    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(Long roomId, Long userId) {
        try {
            log.info("ℹ [채팅방 상세 조회 시작] roomId: {}, userId: {}", roomId, userId);

            // ① 채팅방 조회 (멤버 포함)
            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // ② 멤버 권한 확인
            if (!chatRoom.isMember(userId)) {
                throw new ChatException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
            }

            // ③ 안 읽은 메시지 수
            Long unreadCount = chatRedisService.getUnreadCount(roomId, userId);

            // ④ 마지막 메시지
            Optional<ChatMessage> latestMessage = chatMessageRepository.findLatestMessageByRoomId(roomId);

            // ⑤ 정산 완료 여부 / 리뷰 완료 여부
            Boolean isSettled = null;
            Boolean isReviewed = null;
            Long settlementId = null;
            if (chatRoom.getGroupPost() != null) {
                Long groupPostId = chatRoom.getGroupPost().getId();

                List<Long> activeMemberIds = chatRoom.getMembers().stream()
                        .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                        .map(m -> m.getUser().getId())
                        .collect(Collectors.toList());

                // 게시글의 정산 ID 및 완료 여부
                var settlement = settlementRepository.findByGroupPostId(groupPostId);
                settlementId = settlement.map(s -> s.getId()).orElse(null);
                isSettled = settlement.map(s -> s.getStatus() == SettlementStatus.COMPLETED).orElse(false);

                // 현재 사용자가 다른 모든 활성 멤버에게 리뷰를 남겨야 true
                List<Long> otherMemberIds = activeMemberIds.stream()
                        .filter(id -> !id.equals(userId))
                        .collect(Collectors.toList());
                isReviewed = otherMemberIds.isEmpty() || otherMemberIds.stream()
                        .allMatch(receiverId -> mannerReviewRepository
                                .existsBySenderIdAndReceiverIdAndGroupPostId(userId, receiverId, groupPostId));
            }

            // ⑥ 공통 빌더
            ChatRoomDetailResponse.ChatRoomDetailResponseBuilder builder = ChatRoomDetailResponse.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .roomType(chatRoom.getRoomType().toString())
                    .ownerId(chatRoom.getOwner() != null ? chatRoom.getOwner().getId() : null)
                    .memberCount(
                            (int) chatRoom.getMembers().stream()
                                    .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                                    .count()
                    )
                    .unreadCount(unreadCount)
                    .lastMessage(latestMessage.map(LastMessageDto::from).orElse(null))
                    .createdAt(chatRoom.getCreatedAt())
                    .settlementId(settlementId)
                    .isSettled(isSettled)
                    .isReviewed(isReviewed);

            if (chatRoom.getRoomType() == ChatRoomType.ONE_TO_ONE) {
                // ⑦-A 개인 채팅: roomName을 상대방 이름으로 설정
                ChatMember otherMember = chatRoom.getMembers().stream()
                        .filter(m -> !m.getUser().getId().equals(userId) && m.getStatus() == ChatMemberStatus.ACTIVE)
                        .findFirst()
                        .orElse(null);

                if (otherMember != null) {
                    builder.roomName(otherMember.getUser().getNickname());
                }

                // 멤버 목록 포함
                List<ChatRoomDetailResponse.MemberInfo> memberList = chatRoom.getMembers().stream()
                        .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                        .map(m -> {
                            Long memberId = m.getUser().getId();
                            return ChatRoomDetailResponse.MemberInfo.builder()
                                    .userId(memberId)
                                    .nickname(m.getUser().getNickname())
                                    .profileImage(m.getUser().getProfileImageUrl())
                                    .isOwner(chatRoom.isOwner(memberId))
                                    .build();
                        })
                        .collect(Collectors.toList());
                builder.members(memberList);

                if (chatRoom.getGroupPost() != null) {
                    builder.groupPostId(chatRoom.getGroupPost().getId());
                    try {
                        builder.groupPostTitle(chatRoom.getGroupPost().getTitle());
                    } catch (jakarta.persistence.EntityNotFoundException e) {
                        log.warn(" GroupPost {} 삭제됨, title null 처리", chatRoom.getGroupPost().getId());
                    }
                }

            } else {
                // ⑦-B 단체 채팅: members 목록, groupPost 정보
                List<ChatRoomDetailResponse.MemberInfo> memberList = chatRoom.getMembers().stream()
                        .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                        .map(m -> {
                            Long memberId = m.getUser().getId();
                            return ChatRoomDetailResponse.MemberInfo.builder()
                                    .userId(memberId)
                                    .nickname(m.getUser().getNickname())
                                    .profileImage(m.getUser().getProfileImageUrl())
                                    .isOwner(chatRoom.isOwner(memberId))
                                    .build();
                        })
                        .collect(Collectors.toList());

                builder.members(memberList);

                if (chatRoom.getGroupPost() != null) {
                    builder.groupPostId(chatRoom.getGroupPost().getId());
                    try {
                        builder.groupPostTitle(chatRoom.getGroupPost().getTitle());
                    } catch (jakarta.persistence.EntityNotFoundException e) {
                        log.warn(" GroupPost {} 삭제됨, title null 처리", chatRoom.getGroupPost().getId());
                    }
                }
            }

            log.info(" [채팅방 상세 조회 완료] roomId: {}, roomType: {}", roomId, chatRoom.getRoomType());
            return builder.build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error(" 채팅방 상세 조회 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new RuntimeException("채팅방 상세 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 자리가 빌 때: 해당 게시글을 저장한 유저들에게 POST_UPDATE 알림 발송 (이탈자 제외)
     */
    private void notifySavedPostUsers(Long postId, Long excludeUserId) {
        try {
            List<SavedPost> savedPosts = savedPostRepository.findByPostId(postId);
            for (SavedPost sp : savedPosts) {
                if (sp.getUser().getId().equals(excludeUserId)) continue;
                try {
                    notificationService.createAndSend(
                            sp.getUser(),
                            "POST_UPDATE",
                            "자리가 생겼어요!",
                            "저장한 공동구매에 자리가 생겼습니다.",
                            Map.of("type", "POST_UPDATE", "postId", String.valueOf(postId))
                    );
                } catch (Exception e) {
                    log.warn("저장 게시글 자리 알림 실패 - postId: {}, userId: {}, error: {}",
                            postId, sp.getUser().getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("저장 게시글 자리 알림 전체 실패 - postId: {}, error: {}", postId, e.getMessage());
        }
    }
}
