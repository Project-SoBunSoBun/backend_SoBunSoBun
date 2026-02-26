package com.sobunsobun.backend.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 매너 평가 태그 Enum
 *
 * 클라이언트 ↔ DB 간 tag_code 문자열("TAG001" 등)로 저장되며,
 * 서비스 레이어에서 이 Enum으로 변환하여 사용합니다.
 *
 * id: 프론트엔드 tagId 필드와 매핑되는 숫자 식별자
 */
@Getter
@RequiredArgsConstructor
public enum MannerTag {

    TAG001(1, "시간 약속을 잘 지켜요"),
    TAG002(2, "친절하고 매너가 좋아요"),
    TAG003(3, "응답이 빨라요"),
    TAG004(4, "물건 상태가 설명과 같았어요"),
    TAG005(5, "거래 장소를 잘 알려줬어요"),
    TAG006(6, "가격이 투명하고 합리적이에요");

    /** 프론트엔드 tagId와 1:1 매핑되는 숫자 ID */
    private final int id;

    /** 사용자에게 노출되는 태그 라벨 */
    private final String label;

    /**
     * 숫자 ID로 MannerTag 조회
     */
    public static MannerTag fromId(int id) {
        for (MannerTag tag : values()) {
            if (tag.id == id) {
                return tag;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 태그 ID: " + id);
    }
}
