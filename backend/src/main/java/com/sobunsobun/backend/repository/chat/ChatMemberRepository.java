package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findByRoomId(Long roomId);

    List<ChatMember> findByMemberId(Long memberId);

    List<ChatMember> findByRoomIdIn(Collection<Long> roomIds);

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    void deleteByRoomIdAndMemberId(Long roomId, Long memberId);

    void deleteByRoomIdInAndMemberId(Collection<Long> roomIds, Long memberId);

    long countByRoomId(Long roomId);
}
