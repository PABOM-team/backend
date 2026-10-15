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
                "1. Compile source",
                "2. Run automated tests",
                "3. Build executable JAR and verify Flyway migrations",
                "4. Validate application startup and migrations on MySQL 8.4",
                "BOOT-INF/classes/db/migration",
                "mysql:8.4"
        );
    }
}
