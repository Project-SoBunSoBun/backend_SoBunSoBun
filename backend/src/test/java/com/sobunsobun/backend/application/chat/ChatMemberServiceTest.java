package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMemberServiceTest {

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @InjectMocks
    private ChatMemberService chatMemberService;

    private ChatMember testMember;

    @BeforeEach
    void setUp() {
        testMember = ChatMember.builder()
                .id(1L)
                .roomId(100L)
                .status(ChatMemberStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("멤버 나가기 - soft delete 성공")
    void removeMember_success() {
        // given
        Long roomId = 100L;
        Long userId = 1L;
        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(testMember));
        when(chatMemberRepository.save(any(ChatMember.class)))
                .thenReturn(testMember);

        // when
        chatMemberService.removeMember(roomId, userId);

        // then
        assertEquals(ChatMemberStatus.LEFT, testMember.getStatus());
        assertNotNull(testMember.getLeftAt());
        assertTrue(testMember.getLeftAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(chatMemberRepository, times(1)).save(testMember);
    }

    @Test
    @DisplayName("멤버 나가기 - 멤버가 아닌 경우 예외 발생")
    void removeMember_notMember_throwsException() {
        // given
        Long roomId = 100L;
        Long userId = 999L;
        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.removeMember(roomId, userId));
        
        assertEquals("채팅방 멤버가 아닙니다.", exception.getMessage());
        verify(chatMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 멤버 수 조회")
    void countMembersInRoom() {
        // given
        Long roomId = 100L;
        when(chatMemberRepository.countByRoomIdAndStatus(roomId, ChatMemberStatus.ACTIVE))
                .thenReturn(5L);

        // when
        long count = chatMemberService.countMembersInRoom(roomId);

        // then
        assertEquals(5L, count);
        verify(chatMemberRepository, times(1))
                .countByRoomIdAndStatus(roomId, ChatMemberStatus.ACTIVE);
    }
}
