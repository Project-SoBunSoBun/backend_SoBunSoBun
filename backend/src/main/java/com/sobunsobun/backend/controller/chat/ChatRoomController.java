package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.dto.chat.ChatMessagePage;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.dto.chat.LeaveChatRoomsRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping(value = "/api/chat/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "roomImage", required = false) MultipartFile roomImage) {
        Long userId = principal.id();

        // JSON 문자열을 CreateChatRoomRequest 객체로 변환
        CreateChatRoomRequest request;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            request = objectMapper.readValue(requestJson, CreateChatRoomRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("요청 데이터 파싱 실패: " + e.getMessage());
        }

        ChatRoomResponse response = chatRoomService.createChatRoom(userId, request, roomImage);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/chat/rooms", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatRoomResponse> createChatRoomWithJson(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody CreateChatRoomRequest request) {
        Long userId = principal.id();
        ChatRoomResponse response = chatRoomService.createChatRoom(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/chat/rooms/{roomId}")
    public ResponseEntity<ChatRoomResponse> getChatRoomDetail(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId) {
        Long userId = principal.id();
        ChatRoomResponse response = chatRoomService.getChatRoomDetail(userId, roomId);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/chat/rooms/{roomId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateChatRoomImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId,
            @RequestPart("roomImage") MultipartFile roomImage) {
        Long userId = principal.id();
        chatRoomService.updateChatRoomImage(userId, roomId, roomImage);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRooms(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long userId = principal.id();
        List<ChatRoomResponse> rooms = chatRoomService.getMyRooms(userId);

        return ResponseEntity.ok(rooms);
    }

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

    @DeleteMapping("/api/chat/rooms/{roomId}")
    public ResponseEntity<Void> leaveChatRoom(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId) {
        Long userId = principal.id();
        chatRoomService.leaveChatRoom(userId, roomId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/chat/rooms")
    public ResponseEntity<Void> leaveChatRooms(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody LeaveChatRoomsRequest request) {
        Long userId = principal.id();
        chatRoomService.leaveChatRooms(userId, request.getRoomIds());

        return ResponseEntity.noContent().build();
    }

    // TODO: 채팅에서 사진, 정산서 보내기

    // TODO: 채팅방 초대 기능, 초대 목록 (entity 추가)

    // TODO: 채팅방 멤버 강퇴 기능 (방장 권한)

}
