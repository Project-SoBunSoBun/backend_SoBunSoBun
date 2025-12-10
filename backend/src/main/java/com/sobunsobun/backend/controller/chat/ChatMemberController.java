package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMemberService;
import com.sobunsobun.backend.dto.chat.ChatMemberRequest;
import com.sobunsobun.backend.dto.chat.ChatMemberResponse;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ChatMemberController {

    private final ChatMemberService chatMemberService;

    /**
     * 채팅방 멤버 초대
     */
    @PostMapping("/api/chat/rooms/{roomId}/members")
    public ResponseEntity<ChatMemberResponse> inviteChatMember(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId,
            @RequestBody ChatMemberRequest request) {
        Long userId = principal.id();
        ChatMember invitedMember = chatMemberService.inviteMember(roomId, userId, request);
        return ResponseEntity.ok(ChatMemberResponse.from(invitedMember));
    }

    /**
     * 특정 채팅방의 초대된 멤버 목록 조회
     */
    @GetMapping("/api/chat/rooms/{roomId}/invitations")
    public ResponseEntity<List<ChatMemberResponse>> getInvitedMembers(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId) {
        Long userId = principal.id();
        // 멤버십 확인
        chatMemberService.validateMembership(roomId, userId);
        List<ChatMemberResponse> invitedMembers = chatMemberService.getInvitedMembers(roomId)
                .stream()
                .map(ChatMemberResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invitedMembers);
    }

    /**
     * 사용자가 받은 초대 목록 조회
     */
    @GetMapping("/api/chat/invitations")
    public ResponseEntity<List<ChatMemberResponse>> getMyInvitations(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long userId = principal.id();
        List<ChatMemberResponse> invitations = chatMemberService.getInvitationsByUserId(userId)
                .stream()
                .map(ChatMemberResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invitations);
    }

    /**
     * 초대 수락
     */
    @PostMapping("/api/chat/rooms/{roomId}/invitations/accept")
    public ResponseEntity<ChatMemberResponse> acceptInvitation(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId) {
        Long userId = principal.id();
        ChatMember member = chatMemberService.acceptInvitation(roomId, userId);
        return ResponseEntity.ok(ChatMemberResponse.from(member));
    }

    /**
     * 초대 거절 또는 취소
     */
    @DeleteMapping("/api/chat/rooms/{roomId}/invitations/{memberId}")
    public ResponseEntity<Void> deleteInvitation(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable("roomId") Long roomId,
            @PathVariable("memberId") Long memberId) {
        Long userId = principal.id();
        chatMemberService.deleteInvitation(roomId, userId, memberId);
        return ResponseEntity.ok().build();
    }

    // TODO: 채팅방 멤버 강퇴 기능 (방장 권한)
}
