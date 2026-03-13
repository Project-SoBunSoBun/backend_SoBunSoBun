package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.MannerReview;
import com.sobunsobun.backend.domain.MannerTag;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.manner.MannerReviewRequest;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.MannerReviewRepository;
import com.sobunsobun.backend.repository.UserTagStatsRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 매너 평가 서비스
 *
 * [트랜잭션 설계]
 * submitMannerReviews() 하나의 @Transactional 안에서:
 *   1. 각 평가 대상(receiver)마다 MannerReview(로그) 저장
 *   2. UserTagStats(통계) UPSERT
 * 모든 대상이 하나의 트랜잭션으로 묶여 일관성을 보장합니다.
 *
 * [통계 동기화 전략 - Best Practice]
 * Dirty Checking 방식 (find → count++ → save) 대신
 * 네이티브 UPSERT (INSERT ... ON DUPLICATE KEY UPDATE count = count + 1) 사용:
 * - 단 1쿼리로 원자적 처리
 * - SELECT 없이 동작하므로 동시 요청(race condition) 안전
 * - 대량 트래픽에서도 DB 레벨 락 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MannerReviewService {

    private final MannerReviewRepository mannerReviewRepository;
    private final UserTagStatsRepository userTagStatsRepository;
    private final UserRepository userRepository;
    private final GroupPostRepository groupPostRepository;

    /**
     * 매너 평가 제출 (여러 명 일괄 처리)
     *
     * 처리 순서:
     * 1. 공통 검증 (거래 존재 여부, sender 존재 여부)
     * 2. 각 대상(receiver)마다:
     *    a. 자기 자신 평가 불가 검증
     *    b. receiver 존재 여부 검증
     *    c. 각 태그에 대해 중복 체크 후 MannerReview 로그 저장
     *    d. UserTagStats UPSERT로 통계 즉시 반영
     *
     * @param request  평가 요청 DTO (groupPostId + reviews 목록)
     * @param senderId 평가를 남기는 현재 로그인 사용자 ID
     * @return receiverId → 실제로 저장된 태그 코드 목록 (중복 태그는 제외)
     */
    @Transactional
    public Map<Long, List<String>> submitMannerReviews(MannerReviewRequest request, Long senderId) {
        log.info("매너 평가 요청 - senderId: {}, postId: {}, 대상 수: {}",
            senderId, request.getGroupPostId(), request.getReviews().size());

        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        GroupPost groupPost = groupPostRepository.findById(request.getGroupPostId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래 게시글을 찾을 수 없습니다."));

        Map<Long, List<String>> result = new LinkedHashMap<>();

        for (MannerReviewRequest.ReviewItem item : request.getReviews()) {
            Long receiverId = item.getReceiverId();

            // 자기 자신 평가 불가
            if (senderId.equals(receiverId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 평가할 수 없습니다.");
            }

            User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "평가 대상 사용자를 찾을 수 없습니다. receiverId: " + receiverId));

            List<String> savedTagCodes = new ArrayList<>();

            for (String tagCodeStr : item.getTagCodes()) {
                MannerTag tag;
                try {
                    tag = MannerTag.fromCode(tagCodeStr);
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 태그 코드 요청 - tagCode: {}", tagCodeStr);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 태그 코드입니다: " + tagCodeStr);
                }

                // 동일 거래 + 동일 태그 중복 평가 방지 (DB UNIQUE 제약의 사전 방어)
                if (mannerReviewRepository.existsBySenderIdAndReceiverIdAndGroupPostIdAndTagCode(
                        senderId, receiverId, request.getGroupPostId(), tag)) {
                    log.info("중복 태그 평가 스킵 - senderId: {}, receiverId: {}, tag: {}", senderId, receiverId, tag);
                    continue;
                }

                // 1단계: 리뷰 로그 저장
                MannerReview review = MannerReview.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .groupPost(groupPost)
                    .tagCode(tag)
                    .build();
                mannerReviewRepository.save(review);

                // 2단계: 통계 UPSERT (atomic, 1쿼리)
                userTagStatsRepository.upsertCount(receiverId, tag.name());

                savedTagCodes.add(tag.name());
                log.debug("매너 태그 저장 완료@ - tag: {}, receiverId: {}", tag, receiverId);
            }

            result.put(receiverId, savedTagCodes);
        }

        log.info("매너 평가 완료 - senderId: {}, result: {}", senderId, result);
        return result;
    }
}
