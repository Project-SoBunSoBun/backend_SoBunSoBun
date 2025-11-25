package com.sobunsobun.backend.service.chat;

import com.sobunsobun.backend.dto.chat.ChatMessageRequest;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.entity.chat.ChatMessage;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.exception.ChatAuthException;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, ChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(""));



        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .senderId(request.getSenderId())
                .type
                .build();
        chatMessageRepository.save(message);

        return MessageResponse.from(message);
    }

    // db 저장
    // redis 저장

}
