package com.pabom.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class GitHubActionsWorkflowTest {

    @Test
    void parsesCiWorkflowWithValidationAndDevDeploymentJobs() throws IOException {
        String source = Files.readString(Path.of(".github/workflows/ci.yml"));

        Object loaded = new Yaml().load(source);

        assertThat(loaded).isInstanceOf(Map.class);
        Map<?, ?> workflow = (Map<?, ?>) loaded;
        assertThat(workflow.get("jobs")).isInstanceOf(Map.class);
        Map<?, ?> jobs = (Map<?, ?>) workflow.get("jobs");
        assertThat(jobs.containsKey("develop-branch-name")).isTrue();
        assertThat(jobs.containsKey("main-branch-name")).isTrue();
        assertThat(jobs.containsKey("build")).isTrue();
        assertThat(jobs.containsKey("deploy-dev")).isTrue();
        assertThat(source).contains(
                "SPRING_PROFILES_ACTIVE: ci",
                "DB_HOST: 127.0.0.1",
                "DB_PORT: 3306",
                "DB_PASSWORD: ci-${{ github.run_id }}-${{ github.run_attempt }}",
                "MYSQL_RANDOM_ROOT_PASSWORD: 'yes'",
                "MYSQL_PASSWORD: ci-${{ github.run_id }}-${{ github.run_attempt }}",
                "mysqladmin ping -h 127.0.0.1 -u$MYSQL_USER -p$MYSQL_PASSWORD",
                "1. Compile source",
                "2. Run automated tests",
                "3. Build executable JAR and verify Flyway migrations",
                "4. Validate application startup and migrations on MySQL 8.4",
                "BOOT-INF/classes/db/migration",
                "mysql:8.4"
        );
        assertThat(source).doesNotContain(
                "SPRING_PROFILES_ACTIVE: local",
                "ci_password",
                "ci_root_password"
        );

        assertThat(source).contains(
                "vars.DEV_DEPLOY_ENABLED == 'true'",
                "DEV_EC2_USER: ubuntu",
                "Verify EC2 access and non-interactive sudo",
                "sudo -n true",
                "sudo -n '$remote_dir/deploy-dev.sh'"
        );
        assertThat(source).doesNotContain("DEV_EC2_USER: ${{ secrets.DEV_EC2_USER }}");

        String ciConfiguration = Files.readString(Path.of("src/main/resources/application-ci.yml"));
        assertThat(ciConfiguration).contains(
                "jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}",
                "username: ${DB_USERNAME}",
                "password: ${DB_PASSWORD}",
                "ddl-auto: validate",
                "enabled: true"
        );
    }

    @Test
    void deploymentFilesMatchTheDevEc2RuntimeAndPreserveSecrets() throws IOException {
        String script = Files.readString(Path.of("deploy/deploy-dev.sh"));
        String service = Files.readString(Path.of("deploy/pabom-dev.service"));

        assertThat(script).contains(
                "app_user=\"deploy\"",
                "app_group=\"deploy\"",
                "app_dir=\"/opt/pabom\"",
                "env_file=\"${env_dir}/pabom-dev.env\"",
                "previous_jar=\"${app_dir}/app.jar.previous\"",
                "rollback_on_failure",
                "health_url=\"http://127.0.0.1:8080/api/v1/health\"",
                "if [ ! -f \"$env_file\" ]",
                "chown root:\"$app_group\" \"$env_file\"",
                "chmod 0640 \"$env_file\""
        );
        assertThat(script).doesNotContain(
                "app_user=\"pabom\"",
                "app_group=\"pabom\"",
                "rm -f \"$env_file\""
        );

        assertThat(service).contains(
                "User=deploy",
                "Group=deploy",
                "WorkingDirectory=/opt/pabom",
                "EnvironmentFile=/etc/pabom/pabom-dev.env",
                "Environment=\"JAVA_TOOL_OPTIONS=-Xms128m -Xmx384m -XX:+ExitOnOutOfMemoryError\"",
                "ExecStart=/usr/bin/java -jar /opt/pabom/app.jar"
        );
        assertThat(service).doesNotContain("User=pabom", "Group=pabom");
    }
}
