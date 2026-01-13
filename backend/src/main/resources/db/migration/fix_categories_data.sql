-- ============================================================================
-- GroupPost categories 컬럼 데이터 정리 스크립트
-- ============================================================================
-- 목적: categories 컬럼이 CHAR(4)에서 VARCHAR(50)으로 변경됨
-- 실행 시점: 애플리케이션 재시작 전 (선택사항)
-- ============================================================================

-- 1. 현재 categories 데이터 확인
SELECT
    id,
    title,
    categories,
    LENGTH(categories) as category_length
FROM group_post
WHERE LENGTH(categories) > 4
ORDER BY LENGTH(categories) DESC;

-- 2. 4자 초과 데이터 개수 확인
SELECT
    COUNT(*) as over_4_chars_count
FROM group_post
WHERE LENGTH(categories) > 4;

-- 3. categories 값 분포 확인
SELECT
    categories,
    COUNT(*) as count,
    LENGTH(categories) as length
FROM group_post
GROUP BY categories
ORDER BY count DESC;

-- ============================================================================
-- 데이터 정리 (필요한 경우만 실행)
-- ============================================================================

-- 옵션 1: 4자 초과 데이터를 4자로 자르기
-- 주의: 데이터 손실 가능성 있음
-- UPDATE group_post
-- SET categories = LEFT(categories, 4)
-- WHERE LENGTH(categories) > 4;

-- 옵션 2: 특정 카테고리 코드로 변경
-- 예: "0001" (기본 카테고리)
-- UPDATE group_post
-- SET categories = '0001'
-- WHERE LENGTH(categories) > 4;

-- 옵션 3: 잘못된 데이터 삭제 (주의!)
-- DELETE FROM group_post
-- WHERE LENGTH(categories) > 4;

-- ============================================================================
-- 검증
-- ============================================================================

-- 정리 후 확인
SELECT
    COUNT(*) as total_posts,
    MAX(LENGTH(categories)) as max_category_length,
    MIN(LENGTH(categories)) as min_category_length
FROM group_post;

-- 카테고리별 분포 재확인
SELECT
    categories,
    COUNT(*) as count
FROM group_post
GROUP BY categories
ORDER BY count DESC
LIMIT 10;

