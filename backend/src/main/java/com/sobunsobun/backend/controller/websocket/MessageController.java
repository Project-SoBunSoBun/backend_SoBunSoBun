package com.sobunsobun.backend.controller.websocket;

import com.sobunsobun.backend.dto.websocket.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessageSendingOperations simpMessageSendingOperations;

    @MessageMapping("/hello")
    public void message(ChatMessage message) {

        simpMessageSendingOperations.convertAndSend("/sub/channel/" + message.getChannelId(), message);
    }

}
