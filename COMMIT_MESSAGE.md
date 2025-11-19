# Git Commit Message

## Commit Title
```
feat: 위치 인증 API 구현 및 검색 기록 로그 기반으로 전환
```

## Commit Body
```
🎯 주요 변경사항

1. 위치 인증 API 구현
   - GET /me/location-verification: 위치 인증 정보 조회
   - PATCH /me/location-verification: 위치 인증 업데이트
   - 24시간 유효기간 자동 계산
   - 만료 여부 및 남은 시간 제공

2. 검색 기록 시스템 변경
   - DB 저장 방식에서 로그 파일 기반으로 전환
   - SearchHistory 엔티티 및 Repository 제거
   - SearchRecommendationService를 로그 전용으로 리팩토링
   - 향후 로그 분석 기반 추천 시스템 구현 예정

3. User 도메인 확장
   - locationVerifiedAt 필드 추가 (위치 인증 시간)
   - 기존 address 필드 활용하여 주소 저장

📝 상세 변경 내역

[Domain]
- User.java: locationVerifiedAt 필드 추가
- SearchHistory.java: 삭제 (DB 미사용)

[DTO]
- LocationVerificationRequest: 위치 인증 요청 DTO (주소만 포함)
- LocationVerificationResponse: 위치 인증 응답 DTO (인증 상태, 만료 여부, 남은 시간)

[Service]
- UserService:
  - getLocationVerification(): 위치 인증 정보 조회 및 만료 계산
  - updateLocationVerification(): 위치 인증 업데이트
- SearchRecommendationService:
  - saveSearchHistory(): 로그 파일에만 기록
  - getRecommendations(): 빈 목록 반환 (추후 구현 예정)
  - getPopularKeywords(): 빈 목록 반환
  - getRecentSearches(): 빈 목록 반환

[Controller]
- MeController:
  - GET /me/location-verification 추가
  - PATCH /me/location-verification 추가

[Repository]
- SearchHistoryRepository: 삭제

🗄️ 데이터베이스 마이그레이션

ALTER TABLE `user` 
ADD COLUMN `location_verified_at` TIMESTAMP NULL COMMENT '위치 인증 일시';

📚 문서
- LOCATION_VERIFICATION_API.md: API 사용 가이드
- DATABASE_MIGRATION_GUIDE.md: 마이그레이션 가이드
- ADD_LOCATION_VERIFIED_AT.sql: 마이그레이션 SQL

🔧 기술 스택
- Spring Boot 3.5.4
- JPA/Hibernate
- MySQL

✅ 테스트 상태
- 컴파일: 성공
- 빌드: 성공
```

## English Version (Optional)

```
feat: Implement location verification API and migrate search history to log-based system

🎯 Major Changes

1. Location Verification API
   - GET /me/location-verification: Retrieve location verification info
   - PATCH /me/location-verification: Update location verification
   - Auto-calculate 24-hour validity period
   - Provide expiration status and remaining time

2. Search History System Migration
   - Migrated from DB storage to log-based system
   - Removed SearchHistory entity and Repository
   - Refactored SearchRecommendationService for log-only storage
   - Log-based recommendation system planned for future implementation

3. User Domain Extension
   - Added locationVerifiedAt field (location verification timestamp)
   - Utilized existing address field for address storage

📝 Detailed Changes

[Domain]
- User.java: Added locationVerifiedAt field
- SearchHistory.java: Removed (no DB usage)

[DTO]
- LocationVerificationRequest: Location verification request DTO (address only)
- LocationVerificationResponse: Location verification response DTO (status, expiration, remaining time)

[Service]
- UserService:
  - getLocationVerification(): Query location info and calculate expiration
  - updateLocationVerification(): Update location verification
- SearchRecommendationService:
  - saveSearchHistory(): Log to file only
  - getRecommendations(): Return empty list (to be implemented)
  - getPopularKeywords(): Return empty list
  - getRecentSearches(): Return empty list

[Controller]
- MeController:
  - Added GET /me/location-verification
  - Added PATCH /me/location-verification

[Repository]
- SearchHistoryRepository: Removed

🗄️ Database Migration

ALTER TABLE `user` 
ADD COLUMN `location_verified_at` TIMESTAMP NULL COMMENT 'Location verification timestamp';

📚 Documentation
- LOCATION_VERIFICATION_API.md: API usage guide
- DATABASE_MIGRATION_GUIDE.md: Migration guide
- ADD_LOCATION_VERIFIED_AT.sql: Migration SQL script

🔧 Tech Stack
- Spring Boot 3.5.4
- JPA/Hibernate
- MySQL

✅ Test Status
- Compilation: Success
- Build: Success
```

## Conventional Commits Format

```
feat(user,search): 위치 인증 API 구현 및 검색 기록 로그 기반 전환

BREAKING CHANGE: SearchHistory 테이블이 더 이상 사용되지 않습니다.

- 위치 인증 API (GET/PATCH /me/location-verification) 추가
- User.locationVerifiedAt 필드 추가로 24시간 유효기간 관리
- 검색 기록을 DB 대신 로그 파일로 저장
- SearchHistory 엔티티 및 Repository 제거
```

## Simple Version (For Quick Commit)

```
feat: 위치 인증 API 및 검색 로그 시스템 구현

- 위치 인증 API 추가 (24시간 유효)
- 검색 기록 로그 기반으로 전환
- SearchHistory DB 테이블 제거
```

