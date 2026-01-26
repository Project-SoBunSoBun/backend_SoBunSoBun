-- Comment 테이블에 verify_location 필드 추가
-- 사용자의 위치 인증 정보(주소)를 저장하기 위한 필드

ALTER TABLE `comment` ADD COLUMN `verify_location` VARCHAR(500) COMMENT '사용자의 위치 인증 정보 (주소)' AFTER `content`;

-- verify_location 필드 인덱스 추가 (선택사항)
CREATE INDEX `idx_comment_verify_location` ON `comment`(`verify_location`);

