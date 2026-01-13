package com.sobunsobun.backend.application.search;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import com.sobunsobun.backend.repository.GroupPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 추천 검색어 서비스
 * OPEN 상태의 게시글에서 텍스트를 수집, 토큰화, 빈도 집계하여 추천 검색어 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSuggestionsService {

    private final GroupPostRepository groupPostRepository;

    /**
     * 불용어 목록 (한글 조사, 조동사, 일반 불용어, 의미 없는 단어)
     */
    private static final Set<String> STOP_WORDS = Set.of(
            // 조사
            "이", "그", "저", "것", "수", "등", "같", "때",
            "에", "에서", "에게", "에게서", "으로", "로", "와", "과", "이랑",
            "도", "만", "까지", "부터", "마다", "보다", "같이", "처럼",
            // 조동사/보조동사
            "있", "없", "되", "하", "된", "하다", "있다", "없다", "되다",
            "가", "오", "들", "나", "드", "군", "야", "어",
            // 일반 불용어
            "공동구매", "공구", "구매", "상품", "가능", "합니다", "됩니다",
            "맡깁니다", "물품", "사람", "명", "개", "개월", "주", "일",
            // 의미 없는 단어 (모임/참여 관련)
            "하실분", "모임", "참여", "신청", "테스트", "테스트모임", "관심", "환영",
            "구인", "구직", "채용", "지원", "몇", "정도", "약", "정말",
            "꼭", "무조건", "반드시", "왕", "초", "완전", "진짜", "진정",
            // 짧은 단어들 (음식명 제외하고 의미 없는 단어)
            "아", "응", "음", "네", "예", "안", "줄", "단", "검"
    );

    /**
     * 특수문자를 제거하는 정규표현식
     */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^가-힣a-zA-Z0-9 ]");

    /**
     * 공백 패턴
     */
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 최근 N일 내 OPEN 게시글에서 추천 검색어를 조회
     * 빈도가 높은 단어들을 반환 (중복 포함, 고유 단어만 하나씩)
     *
     * @param days 조회할 기간 (일 단위)
     * @param limit 반환할 추천어 개수
     * @return 추천 검색어 목록 (빈도 순, 고유 단어)
     */
    public List<String> getDefaultSuggestions(int days, int limit) {
        log.info("[추천어] 기본 추천어 조회 시작 - 기간: {}일, limit: {}", days, limit);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // 1. DB에서 최근 N일 OPEN 게시글 조회
        int pageSize = Math.min(limit * 100, 1000);
        Pageable pageable = PageRequest.of(0, pageSize);

        List<GroupPost> posts = groupPostRepository.findRecentOpenPosts(startDate, pageable);
        log.info("[추천어] DB에서 조회한 게시글 수: {}", posts.size());

        if (posts.isEmpty()) {
            log.warn("[추천어] 최근 {}일 내 OPEN 게시글이 없습니다", days);
            return List.of();
        }

        // 2. 텍스트 수집 및 토큰화
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (GroupPost post : posts) {
            String combinedText = combineText(post);
            List<String> tokens = tokenize(combinedText);
            updateFrequency(wordFrequency, tokens);
        }

        log.info("[추천어] 추출된 고유 단어 수: {}", wordFrequency.size());

        // 3. 빈도 순 정렬하여 고유 단어만 limit 개수만 반환
        // (각 단어는 한 번만 반환되지만, 빈도가 높은 것부터 정렬)
        return wordFrequency.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(entry -> {
                    // 로깅: 단어와 빈도 출력
                    log.debug("[추천어] {} (빈도: {})", entry.getKey(), entry.getValue());
                    return entry.getKey();
                })
                .collect(Collectors.toList());
    }

    /**
     * Keyword를 포함하는 추천 검색어를 조회
     *
     * @param keyword 필터링할 키워드
     * @param days 조회할 기간 (일 단위)
     * @param limit 반환할 추천어 개수
     * @return 키워드를 포함하는 추천 검색어 목록
     */
    public List<String> getSuggestionsByKeyword(String keyword, int days, int limit) {
        log.info("[추천어] 키워드 기반 추천어 조회 시작 - 키워드: '{}', 기간: {}일, limit: {}",
                keyword, days, limit);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // 1. DB에서 최근 N일 OPEN 게시글 조회
        int pageSize = Math.min(limit * 100, 1000);
        Pageable pageable = PageRequest.of(0, pageSize);

        List<GroupPost> posts = groupPostRepository.findRecentOpenPosts(startDate, pageable);

        if (posts.isEmpty()) {
            log.warn("[추천어] 최근 {}일 내 OPEN 게시글이 없습니다", days);
            return List.of();
        }

        // 2. 텍스트 수집 및 토큰화
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (GroupPost post : posts) {
            String combinedText = combineText(post);
            List<String> tokens = tokenize(combinedText);
            updateFrequency(wordFrequency, tokens);
        }

        // 3. 키워드를 포함하는 단어만 필터링
        List<String> filtered = wordFrequency.entrySet()
                .stream()
                .filter(entry -> entry.getKey().contains(keyword))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("[추천어] 키워드 필터링 결과 - 매칭 단어 수: {}", filtered.size());
        return filtered;
    }

    /**
     * 게시글의 title만 추출
     */
    private String combineText(GroupPost post) {
        if (post.getTitle() != null && !post.getTitle().isEmpty()) {
            return post.getTitle();
        }
        return "";
    }

    /**
     * 텍스트를 토큰화 (공백/특수문자 기준 분리)
     *
     * 처리 과정:
     * 1. 특수문자 제거
     * 2. 공백으로 분리
     * 3. 최소 길이 2글자 이상만 유지
     * 4. 불용어 제거
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // 1. 특수문자 제거
        String cleaned = SPECIAL_CHAR_PATTERN.matcher(text).replaceAll("");

        // 2. 공백으로 분리
        String[] tokens = SPACE_PATTERN.split(cleaned.trim());

        // 3. 2글자 이상, 불용어 제외
        return Arrays.stream(tokens)
                .filter(token -> token.length() >= 2)
                .filter(token -> !STOP_WORDS.contains(token.toLowerCase()))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 단어 빈도 업데이트
     */
    private void updateFrequency(Map<String, Integer> wordFrequency, List<String> tokens) {
        for (String token : tokens) {
            String lowerToken = token.toLowerCase();
            wordFrequency.put(lowerToken, wordFrequency.getOrDefault(lowerToken, 0) + 1);
        }
    }
}

