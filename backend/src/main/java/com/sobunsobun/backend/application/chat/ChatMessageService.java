package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatMessagePage;
import com.sobunsobun.backend.dto.chat.ChatMessageRequest;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.entity.chat.ChatMessage;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatType;
import com.sobunsobun.backend.infrastructure.chat.RedisChatPublisher;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberService chatMemberService;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisChatPublisher redisChatPublisher;
    private final UserRepository userRepository;

    @Transactional
    public void sendMessage(Long roomId, Long senderId, ChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        // 멤버십 검증
        chatMemberService.validateMembership(roomId, senderId);

        // 발신자 정보 조회
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        Instant now = Instant.now();
        ChatMessage message = ChatMessage.builder()
                .roomId(room.getId())
                .senderId(senderId)
                .type(request.getType() == null ? ChatType.TALK : request.getType())
                .content(request.getContent())
                .createdAt(now)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // 사용자 정보를 포함한 응답 생성
        ChatMessageResponse response = ChatMessageResponse.from(saved, sender);
        redisChatPublisher.publish(room.getId(), response);
    }

    //채팅 목록 가져오기
    @Transactional(readOnly = true)
    public ChatMessagePage getMessages(Long roomId, Long userId, Instant cursor, int size) {
        // 채팅방 존재 확인
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        // 멤버십 검증
        chatMemberService.validateMembership(roomId, userId);

        // 페이지 요청 생성
        PageRequest pageRequest = PageRequest.of(0, size);

        // 커서 기반으로 메시지 조회
        Page<ChatMessage> messagePage;
        if (cursor == null) {
            // 첫 로딩: 최신 메시지부터 size개
            messagePage = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageRequest);
        } else {
            // 커서 페이징: cursor 이전 메시지 size개
            messagePage = chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    roomId, cursor, pageRequest);
        }

        List<ChatMessage> messages = messagePage.getContent();

        // 발신자 정보 일괄 조회 (N+1 방지)
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // ChatMessageResponse 리스트 생성
        List<ChatMessageResponse> responses = messages.stream()
                .map(message -> {
                    User sender = userMap.get(message.getSenderId());
                    return ChatMessageResponse.from(message, sender);
                })
                .toList();

        // hasNext 판단
        boolean hasNext = messagePage.hasNext();

        // nextCursorMillis 설정
        Long nextCursorMillis = null;
        if (hasNext && !messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            nextCursorMillis = lastMessage.getCreatedAt().toEpochMilli();
        }

        return ChatMessagePage.builder()
                .messages(responses)
                .nextCursorMillis(nextCursorMillis)
                .hasNext(hasNext)
                .build();
    }
}