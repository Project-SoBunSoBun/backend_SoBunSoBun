package com.sobunsobun.backend.application.block;

import com.sobunsobun.backend.domain.BlockedUser;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.block.BlockedUserResponse;
import com.sobunsobun.backend.repository.BlockedUserRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.BusinessException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 차단
     *
     * - 자기 자신 차단 불가
     * - 이미 차단된 경우 ALREADY_BLOCKED 예외 (멱등 처리 대신 명시적 피드백 제공)
     */
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ErrorCode.BLOCK_SELF_NOT_ALLOWED);
        }

        if (blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BusinessException(ErrorCode.ALREADY_BLOCKED);
        }

        User blocker = userRepository.findById(blockerId)
            .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
            .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        blockedUserRepository.save(BlockedUser.of(blocker, blocked));
        log.info("사용자 차단 완료: blockerId={}, blockedId={}", blockerId, blockedId);
    }

    /**
     * 차단 취소
     *
     * - 차단 관계가 없으면 BLOCK_NOT_FOUND 예외 (잘못된 호출 명시)
     */
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        if (!blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BusinessException(ErrorCode.BLOCK_NOT_FOUND);
        }

        blockedUserRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        log.info("차단 취소 완료: blockerId={}, blockedId={}", blockerId, blockedId);
    }

    /**
     * 차단 목록 조회
     *
     * - fetch join으로 blocked User를 한 번에 조회해 N+1 방지
     */
    public List<BlockedUserResponse> getBlockedUsers(Long blockerId) {
        return blockedUserRepository.findAllByBlockerIdWithBlocked(blockerId).stream()
            .map(BlockedUserResponse::from)
            .toList();
    }
}
