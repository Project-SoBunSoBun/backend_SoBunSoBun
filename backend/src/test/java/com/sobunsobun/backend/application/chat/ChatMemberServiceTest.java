package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatMemberRequest;
import com.sobunsobun.backend.dto.chat.ChatMemberResponse;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMemberServiceTest {

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatMemberService chatMemberService;

    private ChatMember testMember;
    private ChatMember ownerMember;
    private User testUser;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .profileImageUrl("http://test.com/image.jpg")
                .build();

        ownerUser = User.builder()
                .id(2L)
                .nickname("ownerUser")
                .profileImageUrl("http://test.com/owner.jpg")
                .build();

        // 일반 멤버
        testMember = ChatMember.builder()
                .id(1L)
                .roomId(100L)
                .member(testUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.ACTIVE)
                .build();

        // 방장 멤버
        ownerMember = ChatMember.builder()
                .id(2L)
                .roomId(100L)
                .member(ownerUser)
                .role(ChatMemberRole.OWNER)
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
        assertTrue(testMember.getLeftAt().isBefore(Instant.now().plusSeconds(1)));
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

    // ========== 초대 기능 테스트 ==========

    @Test
    @DisplayName("멤버 초대 - 성공")
    void inviteMember_success() {
        // given
        Long roomId = 100L;
        Long ownerId = 2L;
        User invitedUser = User.builder()
                .id(3L)
                .nickname("invitedUser")
                .build();

        ChatMemberRequest request = new ChatMemberRequest(3L, ChatMemberRole.MEMBER);

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, ownerId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerMember));
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(invitedUser));
        when(chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(roomId, 3L, ChatMemberStatus.ACTIVE))
                .thenReturn(false);
        when(chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(roomId, 3L, ChatMemberStatus.INVITED))
                .thenReturn(false);
        when(chatMemberRepository.save(any(ChatMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMemberResponse result = chatMemberService.inviteMember(roomId, ownerId, request);

        // then
        assertNotNull(result);
        assertEquals(ChatMemberStatus.INVITED, result.getStatus());
        assertEquals(ChatMemberRole.MEMBER, result.getMemberRole());
        assertEquals(3L, result.getUserId());
        assertEquals("invitedUser", result.getNickname());
        verify(chatMemberRepository, times(1)).save(any(ChatMember.class));
    }

    @Test
    @DisplayName("멤버 초대 - 방장이 아닌 경우 예외")
    void inviteMember_notOwner_throwsException() {
        // given
        Long roomId = 100L;
        Long userId = 1L; // 일반 멤버
        ChatMemberRequest request = new ChatMemberRequest(3L, ChatMemberRole.MEMBER);

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(testMember));

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.inviteMember(roomId, userId, request));

        assertEquals("방장만 사용할 수 있는 기능입니다.", exception.getMessage());
        verify(chatMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("멤버 초대 - 존재하지 않는 사용자인 경우 예외")
    void inviteMember_userNotFound_throwsException() {
        // given
        Long roomId = 100L;
        Long ownerId = 2L;
        ChatMemberRequest request = new ChatMemberRequest(999L, ChatMemberRole.MEMBER);

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, ownerId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerMember));
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> chatMemberService.inviteMember(roomId, ownerId, request));

        assertEquals("존재하지 않는 유저입니다.", exception.getMessage());
        verify(chatMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("멤버 초대 - 이미 활성 멤버인 경우 예외")
    void inviteMember_alreadyActiveMember_throwsException() {
        // given
        Long roomId = 100L;
        Long ownerId = 2L;
        User invitedUser = User.builder().id(3L).build();
        ChatMemberRequest request = new ChatMemberRequest(3L, ChatMemberRole.MEMBER);

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, ownerId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerMember));
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(invitedUser));
        when(chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(roomId, 3L, ChatMemberStatus.ACTIVE))
                .thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.inviteMember(roomId, ownerId, request));

        assertEquals("이미 채팅방의 활성 멤버입니다.", exception.getMessage());
        verify(chatMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("멤버 초대 - 이미 초대된 경우 예외")
    void inviteMember_alreadyInvited_throwsException() {
        // given
        Long roomId = 100L;
        Long ownerId = 2L;
        User invitedUser = User.builder().id(3L).build();
        ChatMemberRequest request = new ChatMemberRequest(3L, ChatMemberRole.MEMBER);

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, ownerId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerMember));
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(invitedUser));
        when(chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(roomId, 3L, ChatMemberStatus.ACTIVE))
                .thenReturn(false);
        when(chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(roomId, 3L, ChatMemberStatus.INVITED))
                .thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.inviteMember(roomId, ownerId, request));

        assertEquals("이미 초대된 멤버입니다.", exception.getMessage());
        verify(chatMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("초대된 멤버 목록 조회")
    void getInvitedMembers() {
        // given
        Long roomId = 100L;
        ChatMember invited1 = ChatMember.builder()
                .id(10L)
                .roomId(roomId)
                .member(testUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();
        ChatMember invited2 = ChatMember.builder()
                .id(11L)
                .roomId(roomId)
                .member(ownerUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();

        when(chatMemberRepository.findByRoomIdAndStatus(roomId, ChatMemberStatus.INVITED))
                .thenReturn(List.of(invited1, invited2));

        // when
        List<ChatMemberResponse> result = chatMemberService.getInvitedMembers(roomId);

        // then
        assertEquals(2, result.size());
        assertEquals(ChatMemberStatus.INVITED, result.get(0).getStatus());
        verify(chatMemberRepository, times(1))
                .findByRoomIdAndStatus(roomId, ChatMemberStatus.INVITED);
    }

    @Test
    @DisplayName("사용자가 받은 초대 목록 조회")
    void getInvitationsByUserId() {
        // given
        Long userId = 1L;
        ChatMember invitation1 = ChatMember.builder()
                .id(10L)
                .roomId(100L)
                .member(testUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();
        ChatMember invitation2 = ChatMember.builder()
                .id(11L)
                .roomId(200L)
                .member(testUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();

        when(chatMemberRepository.findByMemberIdAndStatus(userId, ChatMemberStatus.INVITED))
                .thenReturn(List.of(invitation1, invitation2));

        // when
        List<ChatMemberResponse> result = chatMemberService.getInvitationsByUserId(userId);

        // then
        assertEquals(2, result.size());
        assertEquals(ChatMemberStatus.INVITED, result.get(0).getStatus());
        verify(chatMemberRepository, times(1))
                .findByMemberIdAndStatus(userId, ChatMemberStatus.INVITED);
    }

    @Test
    @DisplayName("초대 수락 - 성공")
    void acceptInvitation_success() {
        // given
        Long roomId = 100L;
        Long userId = 1L;
        ChatMember invitation = ChatMember.builder()
                .id(10L)
                .roomId(roomId)
                .member(testUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.INVITED))
                .thenReturn(Optional.of(invitation));
        when(chatMemberRepository.save(any(ChatMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMemberResponse result = chatMemberService.acceptInvitation(roomId, userId);

        // then
        assertEquals(ChatMemberStatus.ACTIVE, result.getStatus());
        verify(chatMemberRepository, times(1)).save(invitation);
    }

    @Test
    @DisplayName("초대 수락 - 초대 정보가 없는 경우 예외")
    void acceptInvitation_invitationNotFound_throwsException() {
        // given
        Long roomId = 100L;
        Long userId = 1L;

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.INVITED))
                .thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.acceptInvitation(roomId, userId));

        assertEquals("초대 정보를 찾을 수 없습니다.", exception.getMessage());
        verify(chatMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("초대 거절 - 본인이 거절하는 경우 성공")
    void deleteInvitation_byInvitedUser_success() {
        // given
        Long roomId = 100L;
        Long userId = 1L;
        Long targetMemberId = 1L; // 본인
        ChatMember invitation = ChatMember.builder()
                .id(10L)
                .roomId(roomId)
                .member(testUser)
                .status(ChatMemberStatus.INVITED)
                .build();

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, targetMemberId, ChatMemberStatus.INVITED))
                .thenReturn(Optional.of(invitation));

        // when
        chatMemberService.deleteInvitation(roomId, userId, targetMemberId);

        // then
        verify(chatMemberRepository, times(1)).delete(invitation);
    }

    @Test
    @DisplayName("초대 취소 - 방장이 취소하는 경우 성공")
    void deleteInvitation_byOwner_success() {
        // given
        Long roomId = 100L;
        Long ownerId = 2L;
        Long targetMemberId = 1L;
        ChatMember invitation = ChatMember.builder()
                .id(10L)
                .roomId(roomId)
                .member(testUser)
                .status(ChatMemberStatus.INVITED)
                .build();

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, targetMemberId, ChatMemberStatus.INVITED))
                .thenReturn(Optional.of(invitation));
        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, ownerId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerMember));

        // when
        chatMemberService.deleteInvitation(roomId, ownerId, targetMemberId);

        // then
        verify(chatMemberRepository, times(1)).delete(invitation);
    }

    @Test
    @DisplayName("초대 삭제 - 권한이 없는 경우 예외")
    void deleteInvitation_noPermission_throwsException() {
        // given
        Long roomId = 100L;
        Long userId = 3L; // 다른 사용자
        Long targetMemberId = 1L;
        ChatMember invitation = ChatMember.builder()
                .id(10L)
                .roomId(roomId)
                .member(testUser)
                .status(ChatMemberStatus.INVITED)
                .build();

        ChatMember otherMember = ChatMember.builder()
                .id(3L)
                .roomId(roomId)
                .member(User.builder().id(3L).build())
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.ACTIVE)
                .build();

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, targetMemberId, ChatMemberStatus.INVITED))
                .thenReturn(Optional.of(invitation));
        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(otherMember));

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.deleteInvitation(roomId, userId, targetMemberId));

        assertEquals("초대를 삭제할 권한이 없습니다.", exception.getMessage());
        verify(chatMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("방장 권한 확인 - 성공")
    void validateRoomOwner_success() {
        // given
        Long roomId = 100L;
        Long ownerId = 2L;

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, ownerId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(ownerMember));

        // when & then
        assertDoesNotThrow(() -> chatMemberService.validateRoomOwner(roomId, ownerId));
    }

    @Test
    @DisplayName("방장 권한 확인 - 방장이 아닌 경우 예외")
    void validateRoomOwner_notOwner_throwsException() {
        // given
        Long roomId = 100L;
        Long userId = 1L;

        when(chatMemberRepository.findByRoomIdAndMemberIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVE))
                .thenReturn(Optional.of(testMember));

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatMemberService.validateRoomOwner(roomId, userId));

        assertEquals("방장만 사용할 수 있는 기능입니다.", exception.getMessage());
    }
}
