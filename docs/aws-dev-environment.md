# AWS DEV environment

The `dev` profile is intended for the EC2 and RDS DEV environment. It keeps the
shared settings in `application.yml` and overrides deployment-specific settings
in `application-dev.yml` without changing the `local` or `prod` profiles.

## Database

Set `DB_URL` to the complete JDBC URL. For the current DEV RDS database:

```text
jdbc:mysql://pabom-dev-db.clqyo66222ox.ap-northeast-2.rds.amazonaws.com:3306/pabom_dev?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

The runtime requires `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Hibernate uses
`ddl-auto: validate`, and Flyway applies the versioned migrations before JPA
validation.

## Runtime environment

Copy `deploy/pabom-dev.env.example` to `/etc/pabom/pabom-dev.env` on EC2,
replace every placeholder, and restrict access to the service account:

```bash
sudo install -d -m 0750 -o root -g pabom /etc/pabom
sudo install -m 0640 -o root -g pabom deploy/pabom-dev.env.example /etc/pabom/pabom-dev.env
sudo editor /etc/pabom/pabom-dev.env
```

Required application variables:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- JWT: `JWT_SECRET` (at least 32 bytes)
- Kakao OAuth: `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`; the DEV callback URL
  defaults to `https://dev.api.pabom.app/api/v1/auth/kakao/callback`
- Google OAuth: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; the DEV callback URL
  defaults to `https://dev.api.pabom.app/api/v1/auth/google/callback`
- S3: `S3_BUCKET`; `AWS_REGION` defaults to `ap-northeast-2`

`AUTH_ALLOWED_ORIGINS` defaults to `https://dev.pabom.app`. The pool, timeout,
logging, callback URL, and presigned URL variables shown in the example file are
optional overrides.

The AWS SDK uses its default credentials provider chain. On EC2, attach an IAM
instance profile granting only the required S3 permissions. Long-lived
`AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` values should not be written to
the environment file.

## Running with the DEV profile

For a built JAR using the EC2 environment file:

```bash
set -a
source /etc/pabom/pabom-dev.env
set +a
java -jar build/libs/app.jar
```

For Gradle during local verification, export the required variables first and
run:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

The supplied `deploy/pabom-dev.service` loads
`/etc/pabom/pabom-dev.env` automatically when started by systemd.
