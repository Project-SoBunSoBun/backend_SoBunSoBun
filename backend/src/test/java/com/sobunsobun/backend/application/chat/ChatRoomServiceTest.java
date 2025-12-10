package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomStatus;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMemberService chatMemberService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        testRoom = new ChatRoom("테스트방", ChatRoomType.GROUP, 1L, null, null);
        ReflectionTestUtils.setField(testRoom, "id", 100L);
        
        // retentionDays 설정 (365일)
        ReflectionTestUtils.setField(chatRoomService, "retentionDays", 365);
    }

    @Test
    @DisplayName("채팅방 나가기 - 멤버가 남아있는 경우")
    void leaveChatRoom_withRemainingMembers() {
        // given
        Long userId = 1L;
        Long roomId = 100L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(chatMemberService.isMember(roomId, userId)).thenReturn(true);
        when(chatMemberService.countMembersInRoom(roomId)).thenReturn(3L);

        // when
        chatRoomService.leaveChatRoom(userId, roomId);

        // then
        verify(chatMemberService, times(1)).removeMember(roomId, userId);
        verify(chatRoomRepository, never()).save(any());
        assertEquals(ChatRoomStatus.OPEN, testRoom.getStatus());
        assertNull(testRoom.getClosedAt());
        assertNull(testRoom.getExpireAt());
    }

    @Test
    @DisplayName("채팅방 나가기 - 마지막 멤버가 나가는 경우 CLOSED 처리")
    void leaveChatRoom_lastMemberLeaving() {
        // given
        Long userId = 1L;
        Long roomId = 100L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(chatMemberService.isMember(roomId, userId)).thenReturn(true);
        when(chatMemberService.countMembersInRoom(roomId)).thenReturn(0L);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testRoom);

        // when
        chatRoomService.leaveChatRoom(userId, roomId);

        // then
        verify(chatMemberService, times(1)).removeMember(roomId, userId);
        verify(chatRoomRepository, times(1)).save(testRoom);
        
        assertEquals(ChatRoomStatus.CLOSED, testRoom.getStatus());
        assertNotNull(testRoom.getClosedAt());
        assertNotNull(testRoom.getExpireAt());

        // expireAt이 closedAt + 365일인지 확인
        Instant expectedExpireAt = testRoom.getClosedAt().plus(365, ChronoUnit.DAYS);
        assertTrue(testRoom.getExpireAt().isAfter(expectedExpireAt.minusSeconds(1)));
        assertTrue(testRoom.getExpireAt().isBefore(expectedExpireAt.plusSeconds(1)));
    }

    @Test
    @DisplayName("채팅방 나가기 - 존재하지 않는 채팅방")
    void leaveChatRoom_roomNotFound() {
        // given
        Long userId = 1L;
        Long roomId = 999L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(EntityNotFoundException.class,
                () -> chatRoomService.leaveChatRoom(userId, roomId));
        
        verify(chatMemberService, never()).removeMember(any(), any());
    }

    @Test
    @DisplayName("채팅방 나가기 - 멤버가 아닌 사용자")
    void leaveChatRoom_notMember() {
        // given
        Long userId = 999L;
        Long roomId = 100L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(chatMemberService.isMember(roomId, userId)).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> chatRoomService.leaveChatRoom(userId, roomId));
        
        verify(chatMemberService, never()).removeMember(any(), any());
    }

    @Test
    @DisplayName("여러 채팅방 나가기 - 일부 채팅방 CLOSED 처리")
    void leaveChatRooms_someRoomsClosed() {
        // given
        Long userId = 1L;
        ChatRoom room1 = new ChatRoom("방1", ChatRoomType.GROUP, 1L, null, null);
        ChatRoom room2 = new ChatRoom("방2", ChatRoomType.GROUP, 1L, null, null);
        ChatRoom room3 = new ChatRoom("방3", ChatRoomType.GROUP, 1L, null, null);
        ReflectionTestUtils.setField(room1, "id", 101L);
        ReflectionTestUtils.setField(room2, "id", 102L);
        ReflectionTestUtils.setField(room3, "id", 103L);
        
        List<Long> roomIds = Arrays.asList(101L, 102L, 103L);
        List<ChatRoom> rooms = Arrays.asList(room1, room2, room3);

        when(chatRoomRepository.findAllById(roomIds)).thenReturn(rooms);
        when(chatMemberService.isMember(any(), eq(userId))).thenReturn(true);
        
        // room1과 room3은 멤버가 0명, room2는 멤버가 1명 남음
        when(chatMemberService.countMembersInRoom(101L)).thenReturn(0L);
        when(chatMemberService.countMembersInRoom(102L)).thenReturn(1L);
        when(chatMemberService.countMembersInRoom(103L)).thenReturn(0L);
        
        when(chatRoomRepository.saveAll(any())).thenReturn(rooms);

        // when
        chatRoomService.leaveChatRooms(userId, roomIds);

        // then
        verify(chatMemberService, times(1)).removeMemberFromRooms(roomIds, userId);
        verify(chatRoomRepository, times(1)).saveAll(any());
        
        // room1과 room3은 CLOSED
        assertEquals(ChatRoomStatus.CLOSED, room1.getStatus());
        assertNotNull(room1.getClosedAt());
        assertNotNull(room1.getExpireAt());
        
        assertEquals(ChatRoomStatus.CLOSED, room3.getStatus());
        assertNotNull(room3.getClosedAt());
        assertNotNull(room3.getExpireAt());
        
        // room2는 OPEN 유지
        assertEquals(ChatRoomStatus.OPEN, room2.getStatus());
        assertNull(room2.getClosedAt());
        assertNull(room2.getExpireAt());
    }

    @Test
    @DisplayName("여러 채팅방 나가기 - 빈 리스트")
    void leaveChatRooms_emptyList() {
        // given
        Long userId = 1L;
        List<Long> roomIds = Arrays.asList();

        // when
        chatRoomService.leaveChatRooms(userId, roomIds);

        // then
        verify(chatRoomRepository, never()).findAllById(any());
        verify(chatMemberService, never()).removeMemberFromRooms(any(), any());
    }
}
