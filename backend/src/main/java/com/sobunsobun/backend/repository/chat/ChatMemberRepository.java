package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

}
