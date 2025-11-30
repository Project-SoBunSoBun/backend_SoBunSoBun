package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.dto.chat.ChatMessageRequest;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.entity.chat.ChatMessage;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatType;
import com.sobunsobun.backend.infrastructure.chat.RedisChatPublisher;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisChatPublisher redisChatPublisher;

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, ChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        validateSender(roomId, senderId);

        Instant now = Instant.now();
        ChatMessage message = ChatMessage.builder()
                .roomId(room.getId())
                .senderId(senderId)
                .type(request.getType() == null ? ChatType.TALK : request.getType())
                .content(request.getContent())
                .createdAt(now)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageResponse response = ChatMessageResponse.from(saved);

        redisChatPublisher.publish(room.getId(), response);
        return response;
    }

    private void validateSender(Long roomId, Long senderId) {
        if (senderId == null) {
            throw new IllegalArgumentException("보낸 사람 정보를 확인할 수 없습니다.");
        }

        boolean isMember = chatMemberRepository
                .existsByRoomIdAndMemberId(roomId, senderId); // 메서드 이름은 실제 레포에 맞게

        if (!isMember) {
            throw new IllegalArgumentException("채팅방 멤버가 아닌 사용자는 메시지를 보낼 수 없습니다.");
        }
    }
}