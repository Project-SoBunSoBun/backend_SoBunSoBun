package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import com.sobunsobun.backend.exception.ChatAuthException;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

        log.info("[채팅방 생성] 완료 - 채팅방 ID: {}, 제목: {}, 멤버 수: {}",
                room.getId(), room.getTitle(), members.size());

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
        log.info("[채팅방 이미지 업데이트] 시작 - 사용자 ID: {}, 채팅방 ID: {}, 이미지 있음: {}",
                userId, roomId, roomImage != null && !roomImage.isEmpty());

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("채팅방 없음 - 채팅방 ID: {}", roomId);
                    return new EntityNotFoundException("존재하지 않는 채팅방입니다.");
                });

        // 2. 권한 확인 (방장만 이미지 변경 가능)
        if (!chatRoom.getOwnerId().equals(userId)) {
            log.warn("채팅방 이미지 수정 권한 없음 - 사용자 ID: {}, 채팅방 ID: {}, 방장 ID: {}",
                    userId, roomId, chatRoom.getOwnerId());
            throw new ChatAuthException("채팅방 이미지는 방장만 변경할 수 있습니다.");
        }

        String oldImageUrl = chatRoom.getImageUrl();
        log.info("기존 채팅방 이미지 URL: {}", oldImageUrl);

        // 3. 새 이미지 업로드
        String newImageUrl = null;
        if (roomImage != null && !roomImage.isEmpty()) {
            try {
                newImageUrl = fileStorageService.saveImage(roomImage);
                log.info("새 채팅방 이미지 업로드 완료 - URL: {}", newImageUrl);
            } catch (ResponseStatusException e) {
                log.error("채팅방 이미지 업로드 실패 {}: {}", e.getClass().getSimpleName(), e.getReason());
                throw e;
            }
        }

        // 4. 채팅방 이미지 URL 업데이트
        chatRoom.setImageUrl(newImageUrl);
        chatRoomRepository.saveAndFlush(chatRoom);
        log.info("DB 업데이트 완료 - 채팅방 이미지 URL이 {}로 변경됨", newImageUrl);

        // 5. 기존 이미지 삭제 (로컬 파일인 경우)
        if (oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(newImageUrl)) {
            log.info("기존 이미지 삭제 시도: {}", oldImageUrl);
            fileStorageService.deleteIfLocal(oldImageUrl);
        }

        log.info("[채팅방 이미지 업데이트] 완료 - 채팅방 ID: {}, 최종 URL: {}", roomId, newImageUrl);
    }

    //void invite(Long roomId, Long targetUserId);
}
