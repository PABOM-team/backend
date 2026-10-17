# 데이터베이스 마이그레이션

## 현재 구성

- 데이터베이스: MySQL 8.4
- 마이그레이션 도구: Flyway
- 마이그레이션 경로: `src/main/resources/db/migration`
- 파일명 규칙: `VyyyyMMddHHmmss__description.sql`
- Hibernate 스키마 정책: `local`, `prod` 환경 모두 `validate`

Flyway는 버전 번호가 낮은 파일부터 순서대로 마이그레이션을 실행하고 결과를
`flyway_schema_history` 테이블에 기록한다. 한 번 적용된 마이그레이션 파일은 수정하지
않으며, 스키마 변경이 필요하면 새로운 버전의 마이그레이션 파일을 추가한다.

초기 마이그레이션은 현재 JPA 엔티티를 기준으로 다음 스키마를 생성한다.

| 테이블 | 주요 제약조건 및 인덱스 |
| --- | --- |
| `users` | `id` PK, (`provider`, `provider_id`) UNIQUE |
| `refresh_tokens` | `id` PK, `token_hash` UNIQUE, `family_id`와 `user_id` 인덱스, `users` FK |
| `user_agreements` | `id` PK, `user_id` 인덱스, `users` FK |
| `events` | `id` PK, `user_id` 인덱스, `users` FK |

현재 코드에는 운영 환경에 필요한 초기 데이터가 없다. 테스트용 데이터는 테스트 코드에서만
관리하며 Flyway 버전 마이그레이션에 추가하지 않는다.

## 신규 데이터베이스 적용

빈 데이터베이스는 별도의 baseline 작업 없이 애플리케이션을 실행하면 Flyway가 초기
마이그레이션부터 자동으로 적용한다.

`baseline-on-migrate`는 `false`로 유지한다. 이 설정은 신규 데이터베이스의 마이그레이션
실행에 영향을 주지 않으며, 잘못된 기존 데이터베이스를 자동으로 baseline하는 상황을 막는다.

## 기존 미관리 데이터베이스가 있는 경우

향후 테이블은 존재하지만 `flyway_schema_history`가 없는 데이터베이스를 Flyway 관리 대상으로
전환해야 한다면 자동 baseline을 사용하지 않고 다음 절차를 따른다.

1. 대상 데이터베이스를 백업한다.
2. 테이블, 컬럼, 자료형, 제약조건 및 인덱스를 초기 마이그레이션과 비교한다.
3. 발견된 차이는 검토를 거친 별도 마이그레이션으로 해결한다.
4. 기존 스키마가 초기 마이그레이션과 기능적으로 동일한 경우에만 `20261011000100` 버전으로
   일회성 baseline을 수행한다. 이 버전은 `users`, `refresh_tokens`, `user_agreements`, `events`
   테이블이 이미 존재하는 상태를 의미한다.
5. 기본 설정으로 애플리케이션을 다시 실행하고 Hibernate 스키마 검증 통과 여부를 확인한다.

로컬 프로필은 다음 환경변수로만 일회성 자동 baseline을 허용한다. 기본값은 항상 `false`이며,
검토가 끝난 기존 로컬 DB의 최초 실행에만 `true`로 설정한다.

```text
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=20261011000100
```

baseline 성공 후에는 `FLYWAY_BASELINE_ON_MIGRATE`를 다시 `false`로 설정한다. 이후 실행은 생성된
`flyway_schema_history`를 사용하므로 자동 baseline이 필요하지 않다. `dev`, `prod`, `ci`는 이
로컬 opt-in 설정을 사용하지 않고 `baseline-on-migrate: false`를 유지한다.

저장소의 코드만으로 확인할 수 없는 로컬 데이터베이스 변경 사항은 추측하여 반영하지 않는다.
또한 로컬 마이그레이션 개발 과정에서 운영 데이터베이스에 직접 접근하거나 변경하지 않는다.

## 운영 영향

- 애플리케이션 시작 시 JPA 초기화 전에 Flyway가 실행된다.
- Hibernate의 로컬 스키마 자동 변경을 사용하지 않으며, 스키마가 일치하지 않으면 시작에
  실패한다.
- 향후 기존 테이블을 변경하는 DDL은 잠금을 발생시킬 수 있으므로 배포 전에 잠금, 데이터
  보정 및 복구 영향을 검토해야 한다.
- 초기 마이그레이션에는 데이터 삭제, 초기화 또는 변경 구문이 없다.
- 운영 DB에 history 테이블이 없다면 로컬 설정을 복사해 자동 baseline하지 않는다. 운영
  스키마와 데이터 무결성을 별도로 점검하고 백업 및 승인된 일회성 baseline 절차를 사용한다.

## 검증 방법

기존 로컬 Docker 볼륨을 재사용하지 않고 격리된 빈 MySQL 8.4 데이터베이스를 사용한다.

1. 애플리케이션을 실행하고 초기 마이그레이션이 성공하는지 확인한다.
2. `flyway_schema_history`에 `20261011000100` 버전이 성공 상태로 기록됐는지 확인한다.
3. Hibernate 스키마 검증이 통과하는지 확인한다.
4. 위 표에 명시된 PK, FK, UNIQUE 제약조건 및 인덱스를 확인한다.
5. 같은 데이터베이스에서 애플리케이션을 다시 실행하여 마이그레이션이 중복 실행되지 않는지
   확인한다.
