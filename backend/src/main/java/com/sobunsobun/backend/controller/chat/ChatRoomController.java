package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.service.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/api/chat/rooms")
    public ChatRoomResponse createRoom(@AuthenticationPrincipal JwtUserPrincipal principal,
                                       @RequestBody CreateChatRoomRequest request) {
        Long userId = principal.id();
        return chatRoomService.createRoom(request);
    }

    @GetMapping("/api/chat/rooms/{roomId}")
    public ChatRoomDetailResponse getRoom(@AuthenticationPrincipal JwtUserPrincipal principal,
                                          @PathVariable Long roomId) {
        return chatRoomService.getRoomDetail(roomId);
    }
}
