package com.sobunsobun.backend.scheduler;

import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomStatus;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomCleanupSchedulerTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatRoomCleanupScheduler scheduler;

    private ChatRoom expiredRoom1;
    private ChatRoom expiredRoom2;

    @BeforeEach
    void setUp() {
        // 만료된 채팅방 1
        expiredRoom1 = new ChatRoom("만료방1", ChatRoomType.GROUP, 1L, null, null);
        ReflectionTestUtils.setField(expiredRoom1, "id", 100L);
        expiredRoom1.setStatus(ChatRoomStatus.CLOSED);
        expiredRoom1.setClosedAt(Instant.now().minus(400, ChronoUnit.DAYS));
        expiredRoom1.setExpireAt(Instant.now().minus(35, ChronoUnit.DAYS));

        // 만료된 채팅방 2
        expiredRoom2 = new ChatRoom("만료방2", ChatRoomType.PRIVATE, 2L, null, null);
        ReflectionTestUtils.setField(expiredRoom2, "id", 200L);
        expiredRoom2.setStatus(ChatRoomStatus.CLOSED);
        expiredRoom2.setClosedAt(Instant.now().minus(400, ChronoUnit.DAYS));
        expiredRoom2.setExpireAt(Instant.now().minus(35, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("만료된 채팅방 삭제 - 성공")
    void deleteExpiredChatRooms_success() {
        // given
        List<ChatRoom> expiredRooms = Arrays.asList(expiredRoom1, expiredRoom2);
        List<Long> expiredRoomIds = Arrays.asList(100L, 200L);

        when(chatRoomRepository.findByStatusAndExpireAtBefore(eq(ChatRoomStatus.CLOSED), any(Instant.class)))
                .thenReturn(expiredRooms);

        // when
        scheduler.deleteExpiredChatRooms();

        // then
        // 1. ChatMember 삭제 확인
        verify(chatMemberRepository, times(1)).deleteByRoomIdIn(expiredRoomIds);

        // 2. MongoDB 메시지 삭제 확인
        verify(chatMessageRepository, times(1)).deleteByRoomId(100L);
        verify(chatMessageRepository, times(1)).deleteByRoomId(200L);

        // 3. ChatRoom 삭제 확인
        verify(chatRoomRepository, times(1)).deleteAllById(expiredRoomIds);
    }

    @Test
    @DisplayName("만료된 채팅방이 없는 경우")
    void deleteExpiredChatRooms_noExpiredRooms() {
        // given
        when(chatRoomRepository.findByStatusAndExpireAtBefore(eq(ChatRoomStatus.CLOSED), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // when
        scheduler.deleteExpiredChatRooms();

        // then
        verify(chatMemberRepository, never()).deleteByRoomIdIn(any());
        verify(chatMessageRepository, never()).deleteByRoomId(any());
        verify(chatRoomRepository, never()).deleteAllById(any());
    }

    @Test
    @DisplayName("MongoDB 메시지 삭제 실패해도 계속 진행")
    void deleteExpiredChatRooms_mongoDbFailureContinues() {
        // given
        List<ChatRoom> expiredRooms = Arrays.asList(expiredRoom1, expiredRoom2);
        List<Long> expiredRoomIds = Arrays.asList(100L, 200L);

        when(chatRoomRepository.findByStatusAndExpireAtBefore(eq(ChatRoomStatus.CLOSED), any(Instant.class)))
                .thenReturn(expiredRooms);

        // MongoDB 첫 번째 삭제는 실패, 두 번째는 성공
        doThrow(new RuntimeException("MongoDB connection error"))
                .when(chatMessageRepository).deleteByRoomId(100L);
        doNothing().when(chatMessageRepository).deleteByRoomId(200L);

        // when
        scheduler.deleteExpiredChatRooms();

        // then
        // MongoDB 삭제 시도는 모두 이루어짐
        verify(chatMessageRepository, times(1)).deleteByRoomId(100L);
        verify(chatMessageRepository, times(1)).deleteByRoomId(200L);

        // 나머지 작업도 계속 진행됨
        verify(chatMemberRepository, times(1)).deleteByRoomIdIn(expiredRoomIds);
        verify(chatRoomRepository, times(1)).deleteAllById(expiredRoomIds);
    }

    @Test
    @DisplayName("단일 채팅방 만료 삭제")
    void deleteExpiredChatRooms_singleRoom() {
        // given
        List<ChatRoom> expiredRooms = Collections.singletonList(expiredRoom1);
        List<Long> expiredRoomIds = Collections.singletonList(100L);

        when(chatRoomRepository.findByStatusAndExpireAtBefore(eq(ChatRoomStatus.CLOSED), any(Instant.class)))
                .thenReturn(expiredRooms);

        // when
        scheduler.deleteExpiredChatRooms();

        // then
        verify(chatMemberRepository, times(1)).deleteByRoomIdIn(expiredRoomIds);
        verify(chatMessageRepository, times(1)).deleteByRoomId(100L);
        verify(chatRoomRepository, times(1)).deleteAllById(expiredRoomIds);
    }
}
