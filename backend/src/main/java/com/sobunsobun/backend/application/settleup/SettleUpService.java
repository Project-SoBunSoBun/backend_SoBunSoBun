package com.sobunsobun.backend.application.settleup;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.SettleUp;
import com.sobunsobun.backend.domain.SettleUpStatus;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.settleup.SettleUpCreateRequest;
import com.sobunsobun.backend.dto.settleup.SettleUpResponse;
import com.sobunsobun.backend.dto.settleup.SettleUpUpdateRequest;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.SettleUpRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 정산 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 정산 CRUD (생성, 조회, 수정, 삭제)
 * - 정산 목록 조회 (페이징, 필터링)
 * - 정산 상태 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettleUpService {

    private final SettleUpRepository settleUpRepository;
    private final GroupPostRepository groupPostRepository;
    private final UserRepository userRepository;

    /**
     * 정산 생성
     *
     * @param userId 생성자 ID
     * @param request 정산 생성 요청 데이터
     * @return 생성된 정산 정보
     */
    @Transactional
    public SettleUpResponse createSettleUp(Long userId, SettleUpCreateRequest request) {
        log.info("[정산 생성] 사용자 ID: {}, 게시글 ID: {}", userId, request.getGroupPostId());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
                });

        // 2. 공동구매 게시글 조회
        GroupPost groupPost = groupPostRepository.findById(request.getGroupPostId())
                .orElseThrow(() -> {
                    log.error("공동구매 게시글을 찾을 수 없음 - ID: {}", request.getGroupPostId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "공동구매 게시글을 찾을 수 없습니다");
                });

        // 3. 중복 정산 체크 (게시글당 1개의 활성 정산만 허용, 삭제된 정산 제외)
        if (settleUpRepository.existsActiveSettleUpByGroupPostId(request.getGroupPostId())) {
            log.error("이미 등록된 정산 - 게시글 ID: {}", request.getGroupPostId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 정산입니다");
        }

        // 4. 정산 엔티티 생성
        SettleUp settleUp = SettleUp.builder()
                .groupPost(groupPost)
                .settledBy(user)
                .status(SettleUpStatus.UNSETTLED)
                .title(request.getTitle())
                .locationName(request.getLocationName())
                .meetAt(request.getMeetAt())
                .build();

        // 5. 저장
        SettleUp savedSettleUp = settleUpRepository.save(settleUp);
        log.info("[정산 생성 완료] 정산 ID: {}, 사용자 ID: {}", savedSettleUp.getId(), userId);

        return convertToResponse(savedSettleUp);
    }

    /**
     * 정산 단건 조회
     *
     * @param settleUpId 정산 ID
     * @return 정산 정보
     */
    public SettleUpResponse getSettleUp(Long settleUpId) {
        log.info("[정산 조회] 정산 ID: {}", settleUpId);

        SettleUp settleUp = settleUpRepository.findByIdWithDetails(settleUpId)
                .orElseThrow(() -> {
                    log.error("정산을 찾을 수 없음 - ID: {}", settleUpId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "정산을 찾을 수 없습니다");
                });

        return convertToResponse(settleUp);
    }

    /**
     * 공동구매 게시글별 정산 목록 조회
     *
     * @param groupPostId 공동구매 게시글 ID
     * @param activeOnly 활성 정산만 조회 여부
     * @return 정산 목록
     */
    public List<SettleUpResponse> getSettleUpsByGroupPost(Long groupPostId, Boolean activeOnly) {
        log.info("[정산 목록 조회] 게시글 ID: {}, 활성만: {}", groupPostId, activeOnly);

        List<SettleUp> settleUps;
        if (activeOnly != null && activeOnly) {
            settleUps = settleUpRepository.findByGroupPostIdAndStatus(groupPostId, SettleUpStatus.UNSETTLED);
        } else {
            settleUps = settleUpRepository.findByGroupPostId(groupPostId);
        }

        return settleUps.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 정산 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param activeOnly 정산 조회 타입 (1: 미정산(기본값), 2: 정산완료, 0: 전체)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 정산 목록 (페이징)
     */
    public Page<SettleUpResponse> getSettleUpsByUser(Long userId, Integer activeOnly, int page, int size) {
        log.info("[사용자별 정산 목록 조회] 사용자 ID: {}, 조회타입: {}, 페이지: {}, 크기: {}", userId, activeOnly, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SettleUp> settleUps;
        if (activeOnly == null || activeOnly == 1) {
            // 1 또는 null: 미정산(status=1) 정산만
            settleUps = settleUpRepository.findBySettledByIdAndStatus(userId, SettleUpStatus.UNSETTLED, pageable);
        } else if (activeOnly == 0) {
            // 0: 전체 정산 (삭제된 것 제외)
            settleUps = settleUpRepository.findAllBySettledById(userId, pageable);
        } else {
            // 2: 정산완료(status=2) 정산만
            settleUps = settleUpRepository.findBySettledByIdAndStatus(userId, SettleUpStatus.SETTLED, pageable);
        }

        return settleUps.map(this::convertToResponse);
    }

    /**
     * 전체 정산 목록 조회 (페이징)
     *
     * @param activeOnly 정산 조회 타입 (1: 미정산(기본값), 2: 정산완료, 0: 전체)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 정산 목록 (페이징)
     */
    public Page<SettleUpResponse> getAllSettleUps(Integer activeOnly, int page, int size) {
        log.info("[전체 정산 목록 조회] 조회타입: {}, 페이지: {}, 크기: {}", activeOnly, page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<SettleUp> settleUps;
        if (activeOnly == null || activeOnly == 1) {
            // 1 또는 null: 미정산(status=1) 정산만
            settleUps = settleUpRepository.findAllUnsettledWithDetails(pageable);
        } else if (activeOnly == 0) {
            // 0: 전체 정산 (삭제된 것 제외)
            settleUps = settleUpRepository.findAllWithDetails(pageable);
        } else {
            // 2: 정산완료(status=2) 정산만
            settleUps = settleUpRepository.findAllSettledWithDetails(pageable);
        }

        return settleUps.map(this::convertToResponse);
    }

    /**
     * 정산 수정
     *
     * @param userId 수정자 ID
     * @param settleUpId 정산 ID
     * @param request 정산 수정 요청 데이터
     * @return 수정된 정산 정보
     */
    @Transactional
    public SettleUpResponse updateSettleUp(Long userId, Long settleUpId, SettleUpUpdateRequest request) {
        log.info("[정산 수정] 정산 ID: {}, 사용자 ID: {}", settleUpId, userId);

        // 1. 정산 조회
        SettleUp settleUp = settleUpRepository.findByIdWithDetails(settleUpId)
                .orElseThrow(() -> {
                    log.error("정산을 찾을 수 없음 - ID: {}", settleUpId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "정산을 찾을 수 없습니다");
                });

        // 2. 권한 확인 (생성자만 수정 가능)
        if (!settleUp.getSettledBy().getId().equals(userId)) {
            log.error("정산 수정 권한 없음 - 정산 ID: {}, 사용자 ID: {}", settleUpId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정산을 수정할 권한이 없습니다");
        }

        // 3. 정산 정보 수정
        if (request.getStatus() != null) {
            settleUp.setStatus(request.getStatus());
        }
        if (request.getTitle() != null) {
            settleUp.setTitle(request.getTitle());
        }
        if (request.getLocationName() != null) {
            settleUp.setLocationName(request.getLocationName());
        }
        if (request.getMeetAt() != null) {
            settleUp.setMeetAt(request.getMeetAt());
        }

        log.info("[정산 수정 완료] 정산 ID: {}", settleUpId);
        return convertToResponse(settleUp);
    }

    /**
     * 정산 삭제 (논리적 삭제 - status를 3으로 변경)
     *
     * @param userId 삭제자 ID
     * @param settleUpId 정산 ID
     */
    @Transactional
    public void deleteSettleUp(Long userId, Long settleUpId) {
        log.info("[정산 삭제] 정산 ID: {}, 사용자 ID: {}", settleUpId, userId);

        // 1. 정산 조회
        SettleUp settleUp = settleUpRepository.findByIdWithDetails(settleUpId)
                .orElseThrow(() -> {
                    log.error("정산을 찾을 수 없음 - ID: {}", settleUpId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "정산을 찾을 수 없습니다");
                });

        // 2. 권한 확인 (생성자만 삭제 가능)
        if (!settleUp.getSettledBy().getId().equals(userId)) {
            log.error("정산 삭제 권한 없음 - 정산 ID: {}, 사용자 ID: {}", settleUpId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정산을 삭제할 권한이 없습니다");
        }

        // 3. 논리적 삭제 (status를 3으로 변경)
        settleUp.setStatus(SettleUpStatus.DELETED);
        log.info("[정산 삭제 완료 (논리적)] 정산 ID: {}, status: 3", settleUpId);
    }

    /**
     * 정산 상태 변경
     *
     * @param userId 사용자 ID
     * @param settleUpId 정산 ID
     * @param status 변경할 상태 (1: 활성, 2: 비활성, 3: 삭제됨)
     * @return 변경된 정산 정보
     */
    @Transactional
    public SettleUpResponse updateSettleUpStatus(Long userId, Long settleUpId, Integer status) {
        log.info("[정산 상태 변경] 정산 ID: {}, 사용자 ID: {}, 상태: {}", settleUpId, userId, status);

        // 1. 정산 조회
        SettleUp settleUp = settleUpRepository.findByIdWithDetails(settleUpId)
                .orElseThrow(() -> {
                    log.error("정산을 찾을 수 없음 - ID: {}", settleUpId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "정산을 찾을 수 없습니다");
                });

        // 2. 권한 확인 (생성자만 상태 변경 가능)
        if (!settleUp.getSettledBy().getId().equals(userId)) {
            log.error("정산 상태 변경 권한 없음 - 정산 ID: {}, 사용자 ID: {}", settleUpId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정산 상태를 변경할 권한이 없습니다");
        }

        // 3. 상태 변경
        settleUp.setStatus(status);
        log.info("[정산 상태 변경 완료] 정산 ID: {}, 상태: {}", settleUpId, status);

        return convertToResponse(settleUp);
    }

    /**
     * SettleUp 엔티티를 SettleUpResponse로 변환
     *
     * @param settleUp 정산 엔티티
     * @return 정산 응답 DTO
     */
    private SettleUpResponse convertToResponse(SettleUp settleUp) {
        return SettleUpResponse.builder()
                .id(settleUp.getId())
                .groupPostId(settleUp.getGroupPost().getId())
                .groupPostTitle(settleUp.getGroupPost().getTitle())
                .settledById(settleUp.getSettledBy().getId())
                .settledByNickname(settleUp.getSettledBy().getNickname())
                .status(settleUp.getStatus())
                .title(settleUp.getTitle())
                .locationName(settleUp.getLocationName())
                .meetAt(settleUp.getMeetAt())
                .createdAt(settleUp.getCreatedAt())
                .updatedAt(settleUp.getUpdatedAt())
                .build();
    }
}

