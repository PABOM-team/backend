#!/usr/bin/env bash
set -Eeuo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <artifact-path> <checksum-path> <service-path>" >&2
  exit 2
fi

artifact_path="$1"
checksum_path="$2"
source_service="$3"
app_user="deploy"
app_group="deploy"
app_dir="/opt/pabom"
env_dir="/etc/pabom"
env_file="${env_dir}/pabom-dev.env"
app_jar="${app_dir}/app.jar"
next_jar="${app_dir}/app.jar.next"
previous_jar="${app_dir}/app.jar.previous"
service_name="pabom-dev.service"
service_file="/etc/systemd/system/${service_name}"
next_service="/tmp/${service_name}.next"
previous_service="/tmp/${service_name}.previous"
health_url="http://127.0.0.1:8080/api/v1/health"
deployment_activated=false

rollback_on_failure() {
  result=$?
  trap - EXIT
  if [ "$result" -ne 0 ] && [ "$deployment_activated" = true ] && [ -f "$previous_jar" ]; then
    echo "Deployment failed. Restoring the previous application." >&2
    install -m 0644 -o "$app_user" -g "$app_group" "$previous_jar" "$app_jar"
    if [ -f "$previous_service" ]; then
      install -m 0644 -o root -g root "$previous_service" "$service_file"
      systemctl daemon-reload
    fi
    systemctl restart "$service_name" || true
  fi
  exit "$result"
}
trap rollback_on_failure EXIT

if [ "${EUID}" -ne 0 ]; then
  echo "This deployment script must run as root." >&2
  exit 1
fi

for command in curl getent groupadd id install java journalctl sha256sum systemctl useradd; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Required command is missing: $command" >&2
    exit 1
  fi
done

if ! getent group "$app_group" >/dev/null 2>&1; then
  groupadd --system "$app_group"
fi
if ! id "$app_user" >/dev/null 2>&1; then
  useradd --system --gid "$app_group" --home-dir "$app_dir" --shell /usr/sbin/nologin "$app_user"
fi

install -d -m 0755 -o "$app_user" -g "$app_group" "$app_dir"
install -d -m 0750 -o root -g "$app_group" "$env_dir"

# Runtime secrets are provisioned separately and must never be created,
# replaced, or removed by an application deployment.
if [ ! -f "$env_file" ]; then
  echo "Runtime environment file is missing: $env_file" >&2
  exit 1
fi
chown root:"$app_group" "$env_file"
chmod 0640 "$env_file"

expected_checksum=$(tr -d '[:space:]' < "$checksum_path")
actual_checksum=$(sha256sum "$artifact_path" | awk '{print $1}')
if [ "$expected_checksum" != "$actual_checksum" ]; then
  echo "Application JAR checksum mismatch." >&2
  exit 1
fi

install -m 0644 -o "$app_user" -g "$app_group" "$artifact_path" "$next_jar"
install -m 0644 -o root -g root "$source_service" "$next_service"
if [ -f "$app_jar" ]; then
  cp -f "$app_jar" "$previous_jar"
fi
if [ -f "$service_file" ]; then
  cp -f "$service_file" "$previous_service"
fi
deployment_activated=true

mv -f "$next_jar" "$app_jar"
mv -f "$next_service" "$service_file"
chown "$app_user:$app_group" "$app_jar"
chown root:root "$service_file"
rm -f "$next_jar" "$next_service"

systemctl daemon-reload
systemctl enable "$service_name" >/dev/null
systemctl restart "$service_name"

for attempt in $(seq 1 60); do
  if curl --fail --silent --show-error --max-time 3 "$health_url" >/dev/null; then
    echo "Pabom DEV deployment is healthy."
    deployment_activated=false
    exit 0
  fi
  sleep 5
done

echo "Health check failed. Recent service logs:" >&2
journalctl -u "$service_name" --no-pager -n 100 >&2 || true

exit 1
