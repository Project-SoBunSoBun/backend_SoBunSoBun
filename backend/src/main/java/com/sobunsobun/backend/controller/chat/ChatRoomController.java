package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    // 채팅방 만들기
    // 내 채팅방 모두 가져오기
    // 소켓 채팅방 입장

    @PostMapping("/api/chat/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(@AuthenticationPrincipal JwtUserPrincipal principal,
                                                      @RequestBody CreateChatRoomRequest request) {
        Long userId = principal.id();
        ChatRoomResponse response = chatRoomService.createChatRoom(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/chat/rooms/{roomId}")
    public ChatRoomResponse getRoom(@AuthenticationPrincipal JwtUserPrincipal principal,
                                          @PathVariable Long roomId) {
        Long userId = principal.id();
        return chatRoomService.getMyRoom(userId, roomId);
    }

//    @GetMapping("api/chat/rooms/info")
//    public ChatRoomResponse getRoomInfoList(@AuthenticationPrincipal JwtUserPrincipal principal,
//                                            @PathVariable Long roomId) {
//        Long userId = principal.id();
//
//    }
}
