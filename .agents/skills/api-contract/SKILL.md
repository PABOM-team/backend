---
name: api-contract
description: HTTP 요청·응답·에러 코드와 Swagger/OpenAPI 예시를 작성하고 검증합니다.
---

# API 계약 및 Swagger

API를 추가하거나 요청·응답·에러 계약을 변경할 때 사용한다.

1. 메서드·경로, 인증, Query/Path/Body 필드, 필수·Nullable·기본값, 정상 상태, 전체 응답 구조, 에러 상태·코드, 부수 효과를 표로 정리한다.
2. 기존 공통 에러 응답 직렬화 구조와 `ErrorCode` 정의를 일치시킨다. 실제 발생하지 않는 오류를 문서화하지 않는다.
3. springdoc OpenAPI의 `@Operation`, `@Parameter`, `@Schema`, `@ApiResponses`, `@ExampleObject`, 인증 요구사항을 적절히 적용한다.
4. 정상 응답과 **서로 다른 모든 에러 코드**의 완전한 JSON 예시를 제공한다. 같은 HTTP 상태에 여러 에러 코드가 있어도 각각 예시를 작성한다.
5. 예시를 DTO 직렬화 결과, Enum, Nullable 필드, 날짜 형식과 일치시킨다.
6. `/v3/api-docs`를 확인하고 경로·응답 코드·예시를 검증하는 자동화 테스트를 추가한다.

완료 기준: 프론트엔드가 별도 비공식 문서 없이 Swagger UI만으로 API를 연동할 수 있어야 한다.
