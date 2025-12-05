package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomStatus;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import com.sobunsobun.backend.exception.ChatAuthException;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberService chatMemberService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.chat.retention-days:365}")
    private int retentionDays;

    @Transactional
    public ChatRoomResponse createChatRoom(Long ownerId, CreateChatRoomRequest request, MultipartFile roomImage) {
        Set<Long> memberIds = new HashSet<>(request.getMemberIds());
        memberIds.add(ownerId);

        // 1. 채팅방 제목 설정
        setChatRoomTitle(ownerId, request);

        // 2. 채팅방 이미지 업로드 (선택적)
        String imageUrl = fileStorageService.saveImage(roomImage);

        // 3. 채팅방 DB 저장
        ChatRoom room = new ChatRoom(request.getTitle(), request.getType(), ownerId, request.getPostId(), imageUrl);
        room = chatRoomRepository.save(room);

        // 4. 채팅 멤버 저장
        List<ChatMember> members = chatMemberService.saveMembers(room.getId(), ownerId, memberIds);

        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .postId(request.getPostId())
                .title(room.getTitle())
                .imageUrl(room.getImageUrl())
                .chatMembers(members)
                .build();
    }

    @Transactional
    public ChatRoomResponse createChatRoom(Long ownerId, CreateChatRoomRequest request) {
        return createChatRoom(ownerId, request, null);
    }

    public void setChatRoomTitle(Long ownerId, CreateChatRoomRequest request) {
        if (request.getTitle() != null) return;

        if (request.getType().equals(ChatRoomType.PRIVATE)) {
            request.getMemberIds().forEach(userId -> {
                if (!Objects.equals(userId, ownerId)) {
                    User user = userRepository.findById(userId).orElseThrow(
                            () -> new EntityNotFoundException("존재하지 않는 유저입니다.")
                    );

                    request.setTitle(user.getNickname());
                }
            });
        } else {
            // TODO: post의 제목가져오기? 아직 정해지지 않음

        }
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomDetail(Long userId, Long roomId) {
        // 멤버십 검증
        chatMemberService.validateMembership(roomId, userId);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("존재하지 않는 채팅방입니다.")
        );

        List<ChatMember> members = chatMemberService.getMembersByRoomId(roomId);

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .postId(chatRoom.getPostId())
                .title(chatRoom.getTitle())
                .imageUrl(chatRoom.getImageUrl())
                .chatMembers(members)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long userId) {
        List<ChatMember> RoomList = chatMemberService.getMembersByUserId(userId);

        if (RoomList.isEmpty()) {
            return List.of();
        }

        Set<Long> roomIds = RoomList.stream()
                .map(ChatMember::getRoomId)
                .collect(Collectors.toSet());

        List<ChatRoom> rooms = chatRoomRepository.findAllById(roomIds);

        return rooms.stream()
                .map(room -> ChatRoomResponse.builder()
                        .roomId(room.getId())
                        .postId(room.getPostId())
                        .title(room.getTitle())
                        .imageUrl(room.getImageUrl())
                        .build()
                )
                .toList();
    }

    @Transactional
    public void updateChatRoomImage(Long userId, Long roomId, MultipartFile roomImage) {
        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                    new EntityNotFoundException("존재하지 않는 채팅방입니다.")
                );

        // 2. 권한 확인 (방장만 이미지 변경 가능)
        if (!chatRoom.getOwnerId().equals(userId)) {
            throw new ChatAuthException("채팅방 이미지는 방장만 변경할 수 있습니다.");
        }

        String oldImageUrl = chatRoom.getImageUrl();

        // 3. 새 이미지 업로드
        String newImageUrl = null;
        if (roomImage != null && !roomImage.isEmpty()) {
            try {
                newImageUrl = fileStorageService.saveImage(roomImage);
            } catch (ResponseStatusException e) {
                throw e;
            }
        }

        // 4. 채팅방 이미지 URL 업데이트
        chatRoom.setImageUrl(newImageUrl);
        chatRoomRepository.saveAndFlush(chatRoom);

        // 5. 기존 이미지 삭제 (로컬 파일인 경우)
        if (oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(newImageUrl)) {
            fileStorageService.deleteIfLocal(oldImageUrl);
        }
    }

    /**
     * 단일 채팅방 나가기
     */
    @Transactional
    public void leaveChatRoom(Long userId, Long roomId) {
        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("존재하지 않는 채팅방입니다.")
        );

        // 2. 멤버십 확인
        if (!chatMemberService.isMember(roomId, userId)) {
            throw new IllegalArgumentException("채팅방에 속해있지 않습니다.");
        }

        // 3. 채팅방 멤버에서 제거 (soft delete)
        chatMemberService.removeMember(roomId, userId);

        // 4. 채팅방에 남은 활성 멤버 수 확인
        long remainingMembers = chatMemberService.countMembersInRoom(roomId);

        // 5. 모든 멤버가 나간 경우 채팅방 CLOSED 처리 및 만료 시간 설정
        if (remainingMembers == 0) {
            LocalDateTime now = LocalDateTime.now();
            chatRoom.setStatus(ChatRoomStatus.CLOSED);
            chatRoom.setClosedAt(now);
            chatRoom.setExpireAt(now.plusDays(retentionDays));
            chatRoomRepository.save(chatRoom);
        }
    }

    /**
     * 여러 채팅방 나가기
     */
    @Transactional
    public void leaveChatRooms(Long userId, List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }

        // 1. 모든 채팅방 존재 확인
        List<ChatRoom> chatRooms = chatRoomRepository.findAllById(roomIds);
        if (chatRooms.size() != roomIds.size()) {
            throw new EntityNotFoundException("일부 채팅방이 존재하지 않습니다.");
        }

        // 2. 각 채팅방에 대한 멤버십 확인
        for (Long roomId : roomIds) {
            if (!chatMemberService.isMember(roomId, userId)) {
                throw new IllegalArgumentException("채팅방 ID " + roomId + "에 속해있지 않습니다.");
            }
        }

        // 3. 모든 채팅방에서 멤버 제거 (soft delete)
        chatMemberService.removeMemberFromRooms(roomIds, userId);

        // 4. 각 채팅방의 남은 멤버 수 확인 및 빈 채팅방 CLOSED 처리
        LocalDateTime now = LocalDateTime.now();
        List<Long> closedRoomIds = chatRooms.stream()
                .filter(room -> chatMemberService.countMembersInRoom(room.getId()) == 0)
                .peek(room -> {
                    room.setStatus(ChatRoomStatus.CLOSED);
                    room.setClosedAt(now);
                    room.setExpireAt(now.plusDays(retentionDays));
                })
                .map(ChatRoom::getId)
                .toList();

        if (!closedRoomIds.isEmpty()) {
            chatRoomRepository.saveAll(chatRooms.stream()
                    .filter(room -> closedRoomIds.contains(room.getId()))
                    .toList());
        }
    }

    //void invite(Long roomId, Long targetUserId);
}
