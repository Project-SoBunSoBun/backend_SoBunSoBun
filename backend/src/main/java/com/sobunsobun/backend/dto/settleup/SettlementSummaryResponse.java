package com.sobunsobun.backend.dto.settleup;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sobunsobun.backend.domain.Settlement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정산 목록 조회용 요약 응답 (참여자 상세 미포함)
 */
@Getter
@Builder
@Schema(description = "정산 요약 응답")
public class SettlementSummaryResponse {

    @Schema(description = "정산 ID", example = "7")
    private Long id;

    @Schema(description = "글쓴이 userId", example = "12")
    private Long authorId;

    @Schema(description = "게시글 ID", example = "5")
    private Long groupPostId;

    @Schema(description = "게시글 제목", example = "마트 공동구매")
    private String groupPostTitle;

    @Schema(description = "정산 상태", example = "PENDING")
    private String status;

    @Schema(description = "총 정산 금액 (PENDING이면 null)", example = "45000")
    private Long totalAmount;

    @Schema(description = "참여자 수", example = "3")
    private int participantCount;

    @Schema(description = "참여자 목록 (userId, nickname)")
    private List<ParticipantInfo> participants;

    @Schema(description = "만남 장소명", example = "강남역 2번 출구")
    private String locationName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    @Schema(description = "만남 일시")
    private LocalDateTime meetAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    public static SettlementSummaryResponse from(Settlement settlement) {
        List<ParticipantInfo> participantInfos = settlement.getParticipants().stream()
                .map(p -> new ParticipantInfo(p.getUser().getId(), p.getUser().getNickname()))
                .toList();

        return SettlementSummaryResponse.builder()
                .id(settlement.getId())
                .authorId(settlement.getGroupPost().getOwner().getId())
                .groupPostId(settlement.getGroupPost().getId())
                .groupPostTitle(settlement.getGroupPost().getTitle())
                .status(settlement.getStatus().name())
                .totalAmount(settlement.getTotalAmount())
                .participantCount(participantInfos.size())
                .participants(participantInfos)
                .locationName(settlement.getGroupPost().getLocationName())
                .meetAt(settlement.getGroupPost().getMeetAt())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }

    @Getter
    public static class ParticipantInfo {
        @Schema(description = "참여자 userId", example = "42")
        private final Long userId;
        @Schema(description = "참여자 닉네임", example = "소분이")
        private final String nickname;

        public ParticipantInfo(Long userId, String nickname) {
            this.userId = userId;
            this.nickname = nickname;
        }
    }
}
