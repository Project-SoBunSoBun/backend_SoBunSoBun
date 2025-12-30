package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.dto.chat.ChatMessageRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebsocketController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable("roomId") Long roomId,
                            Principal principal,
                            ChatMessageRequest request) {

        Long userId = JwtUserPrincipal.from(principal).id();
        chatMessageService.sendMessage(roomId, userId, request);
    }

    // TODO: 채팅에서 사진, 정산서 보내기
}
