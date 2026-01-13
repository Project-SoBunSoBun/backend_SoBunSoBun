package com.sobunsobun.backend.application.search;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import com.sobunsobun.backend.repository.GroupPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 추천 검색어 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSuggestionsService 테스트")
class SearchSuggestionsServiceTest {

    @Mock
    private GroupPostRepository groupPostRepository;

    @InjectMocks
    private SearchSuggestionsService searchSuggestionsService;

    private List<GroupPost> mockPosts;

    @BeforeEach
    void setUp() {
        mockPosts = new ArrayList<>();
    }

    @Test
    @DisplayName("기본 추천어 조회 - 성공")
    void testGetDefaultSuggestions() {
        // Given
        GroupPost post1 = GroupPost.builder()
                .id(1L)
                .title("치킨 공동구매합니다")
                .itemsText("순살치킨 다리살")
                .locationName("강남역 치킨 가게")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        GroupPost post2 = GroupPost.builder()
                .id(2L)
                .title("간식 세트 공구")
                .itemsText("치킨 피자 음료")
                .locationName("서초동 광장")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        GroupPost post3 = GroupPost.builder()
                .id(3L)
                .title("휴지 대량 구매")
                .itemsText("화장지 휴지 물티슈")
                .locationName("이태원 창고")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        mockPosts.add(post1);
        mockPosts.add(post2);
        mockPosts.add(post3);

        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(mockPosts);

        // When
        List<String> suggestions = searchSuggestionsService.getDefaultSuggestions(7, 10);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.size()).isLessThanOrEqualTo(10);
        // "치킨"과 "휴지"는 높은 빈도로 나타나므로 포함되어야 함
        assertThat(suggestions).contains("치킨", "휴지");
        verify(groupPostRepository, times(1)).findRecentOpenPosts(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    @DisplayName("키워드 기반 추천어 조회 - '치' 포함")
    void testGetSuggestionsByKeyword() {
        // Given
        GroupPost post1 = GroupPost.builder()
                .id(1L)
                .title("치킨 공동구매")
                .itemsText("순살치킨 다리살")
                .locationName("강남역")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        GroupPost post2 = GroupPost.builder()
                .id(2L)
                .title("휴지 대량 구매")
                .itemsText("화장지 물티슈")
                .locationName("서초동")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        mockPosts.add(post1);
        mockPosts.add(post2);

        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(mockPosts);

        // When
        List<String> suggestions = searchSuggestionsService.getSuggestionsByKeyword("치", 7, 10);

        // Then
        assertThat(suggestions).isNotEmpty();
        // 모든 추천어가 "치"를 포함해야 함
        suggestions.forEach(suggestion -> assertThat(suggestion).contains("치"));
        // "치킨"은 포함되어야 함
        assertThat(suggestions).contains("치킨");
        // "휴지"는 포함되지 않아야 함 (키워드 필터링)
        assertThat(suggestions).doesNotContain("휴지");
    }

    @Test
    @DisplayName("불용어 제거 확인")
    void testStopWordsRemoval() {
        // Given
        GroupPost post = GroupPost.builder()
                .id(1L)
                .title("공동구매 가능합니다 지금")
                .itemsText("치킨이 있습니다 훌륭한")
                .locationName("강남역에서 만나요")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        mockPosts.add(post);

        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(mockPosts);

        // When
        List<String> suggestions = searchSuggestionsService.getDefaultSuggestions(7, 20);

        // Then
        // 불용어는 제거되어야 함
        assertThat(suggestions)
                .doesNotContain("공동구매", "가능", "합니다", "있", "지금", "에서", "훌륭한");
        // 의미 있는 단어만 포함되어야 함
        assertThat(suggestions).contains("치킨", "강남역");
    }

    @Test
    @DisplayName("최소 길이 2글자 미만 단어 제거")
    void testMinimumLengthFilter() {
        // Given
        GroupPost post = GroupPost.builder()
                .id(1L)
                .title("a 피 치킨을 구매")
                .itemsText("b c 세제 물품")
                .locationName("역 강남")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        mockPosts.add(post);

        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(mockPosts);

        // When
        List<String> suggestions = searchSuggestionsService.getDefaultSuggestions(7, 20);

        // Then
        // 1글자는 제거되어야 함
        assertThat(suggestions).doesNotContain("a", "b", "c", "피");
        // 2글자 이상은 포함되어야 함
        assertThat(suggestions).contains("치킨", "세제", "강남");
    }

    @Test
    @DisplayName("빈도 순 정렬 확인")
    void testFrequencyOrdering() {
        // Given: "치킨"이 3회, "휴지"가 1회 나타남
        GroupPost post1 = GroupPost.builder()
                .id(1L)
                .title("치킨 치킨")
                .itemsText("치킨")
                .locationName("강남")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        GroupPost post2 = GroupPost.builder()
                .id(2L)
                .title("휴지")
                .itemsText("세제")
                .locationName("서초")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        mockPosts.add(post1);
        mockPosts.add(post2);

        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(mockPosts);

        // When
        List<String> suggestions = searchSuggestionsService.getDefaultSuggestions(7, 10);

        // Then
        // "치킨"이 "휴지"보다 먼저 나와야 함
        int chickIndex = suggestions.indexOf("치킨");
        int tissueIndex = suggestions.indexOf("휴지");
        assertThat(chickIndex).isLessThan(tissueIndex);
    }

    @Test
    @DisplayName("Limit 개수 제한 확인")
    void testLimitRestriction() {
        // Given: 10개 이상의 서로 다른 단어가 있는 게시글
        GroupPost post = GroupPost.builder()
                .id(1L)
                .title("치킨 피자 버거 스테이크 파스타 라면 우동 카레 돈까스 쌀국수")
                .itemsText("")
                .locationName("")
                .status(PostStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        mockPosts.add(post);

        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(mockPosts);

        // When
        List<String> suggestions = searchSuggestionsService.getDefaultSuggestions(7, 5);

        // Then
        // limit은 5개이므로 5개 이하만 반환되어야 함
        assertThat(suggestions.size()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("빈 게시글 목록 처리")
    void testEmptyPostList() {
        // Given
        when(groupPostRepository.findRecentOpenPosts(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(new ArrayList<>());

        // When
        List<String> suggestions = searchSuggestionsService.getDefaultSuggestions(7, 10);

        // Then
        assertThat(suggestions).isEmpty();
    }
}

