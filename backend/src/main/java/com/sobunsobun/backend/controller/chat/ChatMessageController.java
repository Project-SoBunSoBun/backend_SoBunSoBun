package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.dto.chat.ChatMessagePage;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    ChatMessageService chatMessageService;

    @GetMapping("/api/chat/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessagePage> getMessages(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId,
            @RequestParam(value = "cursorMillis", required = false) Long cursorMillis,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        Long userId = principal.id();
        Instant cursor = (cursorMillis == null) ? null : Instant.ofEpochMilli(cursorMillis);
        ChatMessagePage page = chatMessageService.getMessages(roomId, userId, cursor, size);
        return ResponseEntity.ok(page);
    }
}
