package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    // 활성 멤버만 조회 (status가 ACTIVE인 멤버)
    List<ChatMember> findByRoomIdAndStatus(Long roomId, ChatMemberStatus status);

    List<ChatMember> findByMemberIdAndStatus(Long memberId, ChatMemberStatus status);

    List<ChatMember> findByRoomIdInAndStatus(Collection<Long> roomIds, ChatMemberStatus status);

    Optional<ChatMember> findByRoomIdAndMemberIdAndStatus(Long roomId, Long memberId, ChatMemberStatus status);

    boolean existsByRoomIdAndMemberIdAndStatus(Long roomId, Long memberId, ChatMemberStatus status);

    // 활성 멤버 수 조회
    long countByRoomIdAndStatus(Long roomId, ChatMemberStatus status);

    // 여러 상태의 멤버 수 조회 (INVITED + ACTIVE 등)
    int countByRoomIdAndStatusIn(Long roomId, Collection<ChatMemberStatus> statuses);

    // 특정 채팅방의 모든 멤버 삭제 (하드 삭제 - 스케줄러용)
    void deleteByRoomId(Long roomId);

    // 여러 채팅방의 모든 멤버 삭제 (하드 삭제 - 스케줄러용)
    void deleteByRoomIdIn(Collection<Long> roomIds);
}
