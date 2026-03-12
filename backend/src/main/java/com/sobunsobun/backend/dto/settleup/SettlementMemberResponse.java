package com.sobunsobun.backend.dto.settleup;

import com.sobunsobun.backend.domain.chat.ChatMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 정산 화면 진입 시 참여 가능한 멤버 목록 응답
 * GET /api/v1/settlements/{settlementId}/members
 */
@Getter
@Builder
@Schema(description = "정산 참여 가능 멤버 목록")
public class SettlementMemberResponse {

    @Schema(description = "정산 ID")
    private Long settlementId;

    @Schema(description = "채팅방 ACTIVE 멤버 목록 (정산 대상)")
    private List<MemberInfo> members;

    public static SettlementMemberResponse of(Long settlementId, List<ChatMember> activeMembers) {
        return SettlementMemberResponse.builder()
                .settlementId(settlementId)
                .members(activeMembers.stream()
                        .map(MemberInfo::from)
                        .toList())
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "멤버 정보")
    public static class MemberInfo {
        private Long userId;
        private String nickname;
        private String profileImageUrl;

        public static MemberInfo from(ChatMember m) {
            return MemberInfo.builder()
                    .userId(m.getUser().getId())
                    .nickname(m.getUser().getNickname())
                    .profileImageUrl(m.getUser().getProfileImageUrl())
                    .build();
        }
    }
}
