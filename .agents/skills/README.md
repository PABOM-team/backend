# PABOM Codex 스킬 모음 (한국어)

각 폴더의 `SKILL.md`를 프로젝트 `.agents/skills/`에 복사하세요. `name`은 Codex 식별을 위해 영어로 유지했고, 설명과 본문은 한국어로 작성했습니다.

- `api-implementation`: Spring Boot API 구현 절차
- `api-contract`: 요청·응답·에러 코드·Swagger 예시
- `api-testing`: API 테스트 및 회귀 검증
- `database-migration`: Flyway/JPA와 데이터 생명주기
- `backend-review`: 병합 전 코드 리뷰

스킬은 반복 작업 규칙이며 IA 전체 API 명세를 대신하지 않습니다. Codex에 제공할 API별 요청·응답·검증 요구사항은 작업 지시문에 포함하세요. 개체 삭제 API는 별도 요청 전까지 구현하지 않습니다.
