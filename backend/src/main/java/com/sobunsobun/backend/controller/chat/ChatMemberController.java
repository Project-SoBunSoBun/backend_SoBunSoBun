package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMemberService;
import com.sobunsobun.backend.dto.chat.KickMemberResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatMemberController {

    private final ChatMemberService chatMemberService;

    /**
     * 방장이 특정 멤버를 강제 퇴장
     *
     * DELETE /api/chat/rooms/{roomId}/members/{targetUserId}
     */
    @DeleteMapping("/{roomId}/members/{targetUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<KickMemberResponse>> kickMember(
            @PathVariable Long roomId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        KickMemberResponse response = chatMemberService.kickMember(roomId, principal.id(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "멤버가 강퇴되었습니다."));
    }
}
