-- GroupPost 테이블에 verify_location 필드 추가
-- 게시글의 위치 인증 정보(주소)를 저장하기 위한 필드

ALTER TABLE `group_post` ADD COLUMN `verify_location` VARCHAR(500) COMMENT '위치 인증 정보 (주소)' AFTER `location_name`;

-- verify_location 필드 인덱스 추가 (선택사항)
CREATE INDEX `idx_group_post_verify_location` ON `group_post`(`verify_location`);

