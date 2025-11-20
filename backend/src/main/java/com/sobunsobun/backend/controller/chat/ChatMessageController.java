package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.dto.chat.ChatMessageRequest;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.service.chat.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            ChatMessageRequest request) {
//        ChatMessageResponse saved = chatMessageService();
//        messagingTemplate.convertAndSend("/topic/rooms" + roomId, saved);
    }

}
