package com.sobunsobun.backend.support.constant;

/**
 * 애플리케이션 전역 상수 관리
 *
 * 애플리케이션에서 사용되는 모든 상수를 중앙에서 관리합니다.
 */
public class AppConstants {

    // =================================================
    // JWT 관련 상수
    // =================================================
    public static class JWT {
        /** JWT 액세스 토큰 만료 시간 (30분) */
        public static final long ACCESS_TOKEN_TTL = 30L * 60 * 1000;

        /** JWT 리프레시 토큰 만료 시간 (60일) */
        public static final long REFRESH_TOKEN_TTL = 60L * 24 * 60 * 60 * 1000;

        /** JWT 임시 로그인 토큰 만료 시간 (10분) */
        public static final long LOGIN_TOKEN_TTL = 10L * 60 * 1000;

        /** JWT 서명 알고리즘 */
        public static final String SIGNING_ALGORITHM = "HS256";

        private JWT() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // 시간대 및 포맷 관련 상수
    // =================================================
    public static class TimeZone {
        /** 한국 시간대 ID */
        public static final String KST_ZONE = "Asia/Seoul";

        /** 한국 시간대 날짜 형식 */
        public static final String KST_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

        private TimeZone() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // 닉네임 관련 상수
    // =================================================
    public static class Nickname {
        /** 최소 닉네임 길이 */
        public static final int MIN_LENGTH = 1;

        /** 최대 닉네임 길이 */
        public static final int MAX_LENGTH = 40;

        /** 허용되는 닉네임 정규식 (한글, 영문, 숫자) */
        public static final String PATTERN = "^[가-힣a-zA-Z0-9_-]+$";

        private Nickname() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // 파일 관련 상수
    // =================================================
    public static class File {
        /** 최대 파일 크기 (10MB) */
        public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

        /** 허용되는 이미지 확장자 */
        public static final String[] ALLOWED_IMAGE_EXTENSIONS = {
                "jpg", "jpeg", "png", "gif", "webp"
        };

        /** 프로필 이미지 최대 크기 (5MB) */
        public static final long MAX_PROFILE_IMAGE_SIZE = 5L * 1024 * 1024;

        private File() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // OAuth 관련 상수
    // =================================================
    public static class OAuth {
        /** 카카오 OAuth 제공자 이름 */
        public static final String KAKAO = "KAKAO";

        /** 애플 OAuth 제공자 이름 */
        public static final String APPLE = "APPLE";

        private OAuth() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // 매너 점수 관련 상수
    // =================================================
    public static class MannerScore {
        /** 최소 매너 점수 */
        public static final double MIN_SCORE = 0.0;

        /** 최대 매너 점수 */
        public static final double MAX_SCORE = 5.0;

        /** 신규 사용자 초기 매너 점수 */
        public static final double INITIAL_SCORE = 3.5;

        private MannerScore() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // 페이지네이션 관련 상수
    // =================================================
    public static class Pagination {
        /** 기본 페이지 크기 */
        public static final int DEFAULT_PAGE_SIZE = 20;

        /** 최대 페이지 크기 */
        public static final int MAX_PAGE_SIZE = 100;

        /** 기본 페이지 번호 */
        public static final int DEFAULT_PAGE_NUMBER = 0;

        private Pagination() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    // =================================================
    // 검색 관련 상수
    // =================================================
    public static class Search {
        /** 최소 검색어 길이 */
        public static final int MIN_QUERY_LENGTH = 2;

        /** 최대 검색어 길이 */
        public static final int MAX_QUERY_LENGTH = 100;

        /** 검색 제안 최대 개수 */
        public static final int MAX_SUGGESTIONS = 10;

        private Search() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }

    private AppConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}

