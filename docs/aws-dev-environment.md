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

Create `/etc/pabom/pabom-dev.env` once during EC2 provisioning, replace every
placeholder from `deploy/pabom-dev.env.example`, and restrict access to the
service account. Abort if the file already exists so that a deployment cannot
overwrite secrets:

```bash
sudo install -d -m 0750 -o root -g deploy /etc/pabom
sudo test ! -e /etc/pabom/pabom-dev.env
sudo install -m 0640 -o root -g deploy /dev/null /etc/pabom/pabom-dev.env
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

## GitHub Actions deployment access

The DEV workflow uses GitHub OIDC instead of an SSH key. It assumes
`arn:aws:iam::660008587020:role/pabom-dev-github-actions-role`, uploads a bundle
under the commit-specific S3 prefix below, and sends `AWS-RunShellScript` only
to `i-09893c17f40d15314`:

```text
s3://pabom-dev-assets-660008587020-660008587020-ap-northeast-2-an/deploy/dev/<commit-sha>/
```

This bucket name is the currently supplied AWS setting. The repository did not
previously contain a concrete bucket name, and the current development machine
has no AWS credentials with which to verify it. In particular, do not "fix"
the repeated account number or the `-an` suffix based only on their appearance.
Verify the exact name and region in AWS before enabling deployment:

```bash
aws s3api head-bucket \
  --bucket pabom-dev-assets-660008587020-660008587020-ap-northeast-2-an
aws s3api get-bucket-location \
  --bucket pabom-dev-assets-660008587020-660008587020-ap-northeast-2-an
```

The GitHub environment is `dev`. Because the job uses an environment, the OIDC
role trust policy must allow the subject
`repo:PABOM-team/backend:environment:dev` and the audience
`sts.amazonaws.com`. The role permissions must allow `s3:PutObject` only under
the deployment prefix, `ssm:SendCommand` for the AWS-managed document and the
one DEV instance, and command-result reads. For example:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UploadDevRelease",
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::pabom-dev-assets-660008587020-660008587020-ap-northeast-2-an/deploy/dev/*"
    },
    {
      "Sid": "SendCommandToDevInstance",
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript",
        "arn:aws:ec2:ap-northeast-2:660008587020:instance/i-09893c17f40d15314"
      ]
    },
    {
      "Sid": "ReadCommandResult",
      "Effect": "Allow",
      "Action": "ssm:GetCommandInvocation",
      "Resource": "*"
    }
  ]
}
```

## EC2 prerequisites

The SSM agent must be online, and the instance must have an instance profile
with the SSM managed-node permissions (normally
`AmazonSSMManagedInstanceCore`). The instance also needs AWS CLI v2, `curl`, a
Java 21 runtime, and permission to download only deployment objects. If its
current role does not already have that S3 permission, attach an inline policy
equivalent to the following; do not assume application asset permissions also
cover this prefix:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadDevDeploymentBundles",
      "Effect": "Allow",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::pabom-dev-assets-660008587020-660008587020-ap-northeast-2-an/deploy/dev/*"
    }
  ]
}
```

If the bucket uses a customer-managed KMS key, add `kms:Decrypt` for that key
to the instance role and `kms:Encrypt` plus `kms:GenerateDataKey` to the GitHub
role. These permissions are not needed for ordinary S3-managed encryption.

`AWS-RunShellScript` runs as root on Linux. The deployment script validates the
checksum, installs the JAR as `deploy:deploy` at `/opt/pabom/app.jar`, and keeps
`/etc/pabom/pabom-dev.env` owned by `root:deploy` with mode `0640`. It never
prints or replaces that environment file. The systemd unit uses the same user,
group, paths, and `pabom-dev.service` name. On failure it restores the previous
JAR and service definition, restarts the service, and verifies the health URL.

Before changing `DEV_DEPLOY_ENABLED` from `false`, verify all of the following
in AWS and GitHub:

- The bucket exists in `ap-northeast-2` with the exact configured name.
- The OIDC provider, trust subject, and GitHub role permissions match the
  repository, `dev` environment, deployment prefix, and single instance.
- The EC2 instance profile contains SSM managed-node and deployment-prefix S3
  read permissions, and any required KMS decrypt permission.
- `i-09893c17f40d15314` is online in Systems Manager Managed nodes.
- `/etc/pabom/pabom-dev.env` already exists with the required secret values;
  inspect only its ownership and mode, never its contents in CI output.
- The checked-in unit matches the installed runtime account and paths.

Keep `DEV_DEPLOY_ENABLED=false` until these checks pass. The workflow retains
the existing `vars.DEV_DEPLOY_ENABLED == 'true'` guard and does not change the
variable itself.
