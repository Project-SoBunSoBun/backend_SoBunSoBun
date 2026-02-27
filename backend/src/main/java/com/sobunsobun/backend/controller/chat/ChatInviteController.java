package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatInviteService;
import com.sobunsobun.backend.dto.chat.ChatInviteCancelResponse;
import com.sobunsobun.backend.dto.chat.ChatInviteRequest;
import com.sobunsobun.backend.dto.chat.ChatInviteResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Chat - 초대", description = "채팅 초대 발송 및 취소/거절")
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatInviteController {

    private final ChatInviteService chatInviteService;

    /**
     * 그룹 채팅방 초대 발송
     *
     * POST /api/chat/rooms/{roomId}/invites
     */
    @PostMapping("/rooms/{roomId}/invites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChatInviteResponse>> inviteUser(
            @PathVariable Long roomId,
            @RequestBody @Valid ChatInviteRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        ChatInviteResponse response = chatInviteService.invite(roomId, principal.id(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, "초대가 발송되었습니다."));
    }

    /**
     * 초대 취소 또는 거절
     *
     * PATCH /api/chat/invites/{inviteId}/cancel
     * - inviter(발신자): 취소
     * - invitee(수신자): 거절
     */
    @PatchMapping("/invites/{inviteId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChatInviteCancelResponse>> cancelOrRejectInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        ChatInviteCancelResponse response = chatInviteService.cancelOrRejectInvite(inviteId, principal.id());
        return ResponseEntity.ok(ApiResponse.success(response, "초대가 취소/거절되었습니다."));
    }
}
