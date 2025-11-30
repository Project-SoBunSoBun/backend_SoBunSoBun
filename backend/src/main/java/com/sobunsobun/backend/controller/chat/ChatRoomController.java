package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/api/chat/rooms")
    public ResponseEntity<ChatRoomResponse> createChatRoom(@AuthenticationPrincipal JwtUserPrincipal principal,
                                                       @RequestBody CreateChatRoomRequest request) {
        Long userId = principal.id();
        ChatRoomResponse response = chatRoomService.createChatRoom(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/chat/rooms/{roomId}")
    public ResponseEntity<ChatRoomResponse> getChatRoomDetail(@AuthenticationPrincipal JwtUserPrincipal principal,
                                          @PathVariable Long roomId) {
        Long userId = principal.id();
        ChatRoomResponse response = chatRoomService.getChatRoomDetail(userId, roomId);

        return ResponseEntity.ok(response);
    }

    // TODO: 채팅방 리스트 가져오기
//    @GetMapping("api/chat/rooms")
//    public ChatRoomResponse getChatRooms(@AuthenticationPrincipal JwtUserPrincipal principal,
//                                     @PathVariable Long roomId) {
//        Long userId = principal.id();
//    }

    // TODO: 채팅방 채팅 20개 정도씩 짤라서 페이징으로 가져오기

    // TODO: 채팅에서 사진, 정산서 보내기

    // TODO: 채팅방 초대 기능, 채팅방 가입 요청

    // TODO: 채팅방 멤버 강퇴 기능 (방장 권한)


}
