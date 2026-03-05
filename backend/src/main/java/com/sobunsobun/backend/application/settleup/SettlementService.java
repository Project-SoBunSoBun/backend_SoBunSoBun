package com.sobunsobun.backend.application.settleup;

import com.sobunsobun.backend.domain.*;
import com.sobunsobun.backend.domain.chat.ChatMemberStatus;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.dto.settleup.*;
import com.sobunsobun.backend.repository.SettlementRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.exception.SettlementException;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    // =================================================
    // 게시글 생성 시 내부 호출 — 외부 API 없음
    // =================================================

    /**
     * 게시글 생성 시 자동으로 PENDING 상태 정산 생성
     * PostService.createPost() 에서 동일 트랜잭션 안에서 호출
     */
    @Transactional
    public void createForPost(GroupPost groupPost) {
        Settlement settlement = Settlement.createFor(groupPost);
        settlementRepository.save(settlement);
        log.info("[정산 자동 생성] postId={}, settlementId={}", groupPost.getId(), settlement.getId());
    }

    // =================================================
    // 조회
    // =================================================

    /**
     * 정산 상세 조회 (참여자 + 품목 포함)
     */
    public SettlementDetailResponse getSettlementDetail(Long settlementId) {
        Settlement settlement = settlementRepository.findWithDetailById(settlementId)
                .orElseThrow(SettlementException::notFound);
        return SettlementDetailResponse.from(settlement);
    }

    /**
     * 게시글 ID로 정산 상세 조회
     */
    public SettlementDetailResponse getSettlementByPost(Long groupPostId) {
        Settlement settlement = settlementRepository.findWithDetailByGroupPostId(groupPostId)
                .orElseThrow(SettlementException::notFound);
        return SettlementDetailResponse.from(settlement);
    }

    /**
     * 내 정산 목록 (내가 작성한 게시글의 정산)
     *
     * @param status null이면 전체, "PENDING" / "COMPLETED" 필터 가능
     */
    public Page<SettlementSummaryResponse> getMySettlements(Long userId,
                                                            String status,
                                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Settlement> settlements;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            SettlementStatus statusEnum = parseStatus(status);
            settlements = settlementRepository.findByGroupPostOwnerIdAndStatus(userId, statusEnum, pageable);
        } else {
            settlements = settlementRepository.findByGroupPostOwnerId(userId, pageable);
        }

        return settlements.map(SettlementSummaryResponse::from);
    }

    // =================================================
    // 정산 완료
    // =================================================

    /**
     * iOS 최종 계산 결과 수신 후 정산 완료 처리
     *
     * 검증:
     * - 게시글 작성자만 완료 가능
     * - totalAmount == sum(participants[*].assignedAmount)
     * - 요청 참여자 목록 == 채팅방 ACTIVE 멤버 (방식 B)
     * - 이미 완료된 정산은 재완료 가능 (수정 허용)
     */
    @Transactional
    public SettlementDetailResponse completeSettlement(Long userId,
                                                       Long settlementId,
                                                       SettlementCompleteRequest request) {
        // 1. 정산 조회
        Settlement settlement = settlementRepository.findWithDetailById(settlementId)
                .orElseThrow(SettlementException::notFound);

        // 2. 권한 확인: 게시글 작성자만 완료 가능
        if (!settlement.getGroupPost().getOwner().getId().equals(userId)) {
            log.warn("[정산 완료 권한 없음] settlementId={}, userId={}", settlementId, userId);
            throw SettlementException.forbidden();
        }

        // 3. 총액 == 참여자 합계 검증
        long participantSum = request.getParticipants().stream()
                .mapToLong(SettlementParticipantRequest::getAssignedAmount)
                .sum();
        if (participantSum != request.getTotalAmount()) {
            log.warn("[정산 금액 불일치] settlementId={}, sum={}, total={}",
                    settlementId, participantSum, request.getTotalAmount());
            throw SettlementException.amountMismatch(participantSum, request.getTotalAmount());
        }

        // 4. [방식 B] 요청 참여자 == 채팅방 ACTIVE 멤버 검증
        validateParticipantsMatchChatRoom(settlement.getGroupPost().getId(), request);

        // 5. 참여자 + 품목 엔티티 구성
        List<SettlementParticipant> participants = request.getParticipants().stream()
                .map(pReq -> buildParticipant(settlement, pReq))
                .toList();

        // 6. 도메인 메서드로 완료 처리 (orphanRemoval로 기존 데이터 자동 삭제)
        settlement.complete(request.getTotalAmount(), participants);

        log.info("[정산 완료] settlementId={}, totalAmount={}, participants={}",
                settlementId, request.getTotalAmount(), participants.size());

        return SettlementDetailResponse.from(settlement);
    }

    /**
     * [방식 B] iOS가 보낸 참여자 목록이 채팅방 ACTIVE 멤버와 정확히 일치하는지 검증
     */
    private void validateParticipantsMatchChatRoom(Long groupPostId,
                                                    SettlementCompleteRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findByGroupPostIdWithMembers(groupPostId)
                .orElseThrow(() -> new SettlementException(
                        ErrorCode.CHAT_ROOM_NOT_FOUND,
                        "해당 게시글의 단체 채팅방을 찾을 수 없습니다."));

        Set<Long> activeMemberIds = chatRoom.getMembers().stream()
                .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> requestParticipantIds = request.getParticipants().stream()
                .map(SettlementParticipantRequest::getUserId)
                .collect(Collectors.toSet());

        if (!activeMemberIds.equals(requestParticipantIds)) {
            log.warn("[정산 참여자 불일치] groupPostId={}, chatMembers={}, requestIds={}",
                    groupPostId, activeMemberIds, requestParticipantIds);
            throw new SettlementException(ErrorCode.SETTLEUP_PARTICIPANT_MISMATCH,
                    String.format("정산 참여자(%s)가 채팅방 활성 멤버(%s)와 일치하지 않습니다.",
                            requestParticipantIds, activeMemberIds));
        }
    }

    // =================================================
    // Private helpers
    // =================================================

    private SettlementParticipant buildParticipant(Settlement settlement,
                                                   SettlementParticipantRequest pReq) {
        User user = userRepository.findById(pReq.getUserId())
                .orElseThrow(UserException::notFound);

        SettlementParticipant participant =
                SettlementParticipant.of(settlement, user, pReq.getAssignedAmount());

        List<SettlementItem> items = pReq.getItems().stream()
                .map(iReq -> SettlementItem.of(
                        participant,
                        iReq.getItemName(),
                        iReq.getQuantity(),
                        iReq.getUnit(),
                        iReq.getAmount()))
                .toList();

        participant.addItems(items);
        return participant;
    }

    private SettlementStatus parseStatus(String status) {
        try {
            return SettlementStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SettlementException(ErrorCode.INVALID_SETTLEUP_STATUS,
                    "status는 ALL, PENDING, COMPLETED 중 하나여야 합니다.");
        }
    }
}
