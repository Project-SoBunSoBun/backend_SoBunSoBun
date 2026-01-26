package com.sobunsobun.backend.support.util;

import com.sobunsobun.backend.support.constant.AppConstants;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 검증 유틸리티
 *
 * 애플리케이션 전체에서 사용되는 입력값 검증 로직을 중앙화합니다.
 */
@Slf4j
public class ValidationUtil {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile(AppConstants.Nickname.PATTERN);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^01[0-9]-?\\d{3,4}-?\\d{4}$"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://[\\w.-]+(?:\\.[a-z]{2,})+(?:/.*)?$", Pattern.CASE_INSENSITIVE
    );

    private ValidationUtil() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // =================================================
    // 닉네임 검증
    // =================================================

    /**
     * 닉네임 유효성 검증
     *
     * @param nickname 검증할 닉네임
     * @throws UserException 유효하지 않은 닉네임인 경우
     */
    public static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw UserException.invalidNickname("닉네임은 필수입니다.");
        }

        String trimmed = nickname.trim();

        if (trimmed.length() < AppConstants.Nickname.MIN_LENGTH) {
            throw UserException.invalidNickname(
                    "닉네임은 최소 " + AppConstants.Nickname.MIN_LENGTH + "자 이상이어야 합니다."
            );
        }

        if (trimmed.length() > AppConstants.Nickname.MAX_LENGTH) {
            throw UserException.invalidNickname(
                    "닉네임은 최대 " + AppConstants.Nickname.MAX_LENGTH + "자 이하여야 합니다."
            );
        }

        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            throw UserException.invalidNickname(
                    "닉네임은 한글, 영문, 숫자만 사용 가능합니다."
            );
        }
    }

    /**
     * 닉네임 유효성 검증 (예외 없이 boolean 반환)
     *
     * @param nickname 검증할 닉네임
     * @return 유효 여부
     */
    public static boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        String trimmed = nickname.trim();
        return trimmed.length() >= AppConstants.Nickname.MIN_LENGTH &&
               trimmed.length() <= AppConstants.Nickname.MAX_LENGTH &&
               NICKNAME_PATTERN.matcher(trimmed).matches();
    }

    // =================================================
    // 이메일 검증
    // =================================================

    /**
     * 이메일 유효성 검증
     *
     * @param email 검증할 이메일
     * @return 유효 여부
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    // =================================================
    // 전화번호 검증
    // =================================================

    /**
     * 전화번호 유효성 검증
     *
     * @param phone 검증할 전화번호
     * @return 유효 여부
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    // =================================================
    // URL 검증
    // =================================================

    /**
     * URL 유효성 검증
     *
     * @param url 검증할 URL
     * @return 유효 여부
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    // =================================================
    // 숫자 검증
    // =================================================

    /**
     * 숫자 범위 검증
     *
     * @param value 검증할 값
     * @param min 최소값 (포함)
     * @param max 최대값 (포함)
     * @return 범위 내 여부
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * 숫자 범위 검증 (Double)
     *
     * @param value 검증할 값
     * @param min 최소값 (포함)
     * @param max 최대값 (포함)
     * @return 범위 내 여부
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * 양수 검증
     *
     * @param value 검증할 값
     * @return 양수 여부
     */
    public static boolean isPositive(int value) {
        return value > 0;
    }

    /**
     * 양수 검증 (Long)
     *
     * @param value 검증할 값
     * @return 양수 여부
     */
    public static boolean isPositive(long value) {
        return value > 0;
    }

    // =================================================
    // 컬렉션 검증
    // =================================================

    /**
     * 문자열 배열이 비어있지 않은지 검증
     *
     * @param array 검증할 배열
     * @return 비어있지 않으면 true
     */
    public static boolean isNotEmpty(String[] array) {
        return array != null && array.length > 0;
    }

    /**
     * 문자열이 비어있지 않은지 검증
     *
     * @param str 검증할 문자열
     * @return 비어있지 않으면 true
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * 문자열이 특정 길이 범위 내인지 검증
     *
     * @param str 검증할 문자열
     * @param minLength 최소 길이
     * @param maxLength 최대 길이
     * @return 범위 내이면 true
     */
    public static boolean isLengthInRange(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength <= 0;
        }
        return str.length() >= minLength && str.length() <= maxLength;
    }
}

