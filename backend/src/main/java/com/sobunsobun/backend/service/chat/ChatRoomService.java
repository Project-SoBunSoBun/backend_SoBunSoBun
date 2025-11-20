package com.sobunsobun.backend.service.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;

    private final UserRepository userRepository;


    @Transactional
    public void createChatRoom(Long ownerId, CreateChatRoomRequest request) {
        Set<Long> memberIds = new HashSet<>(request.getMemberIds());
        memberIds.add(ownerId);

        if (request.getType() == ChatRoomType.PRIVATE && memberIds.size() != 2) {
            throw new IllegalArgumentException("1:1 채팅방은 정확히 두 명이어야 합니다.");
        }
        ChatRoom room = new ChatRoom(request.getTitle(), request.getType(), ownerId);
        room = chatRoomRepository.save(room);

        List<ChatMember> memberList = new ArrayList<>();
        for (Long memberId :  memberIds) {
            User user = userRepository.findById(memberId).orElseThrow(
                    () -> new EntityNotFoundException("존재하지 않는 유저입니다.")
            );
            ChatMember chatMember = ChatMember.builder()
                    .roomId(room.getId())
                    .member(user)
                    .build();

            memberList.add(chatMember);
        }
        chatMemberRepository.saveAll(memberList);
    }



    //getRoom(Long roomId)
    //List<ChatRoomSummary> getMyRooms(Long userId);
    //void invite(Long roomId, Long targetUserId);
}
