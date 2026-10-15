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
    void parsesCiWorkflowWithBuildAndDevDeploymentJobs() throws IOException {
        String source = Files.readString(Path.of(".github/workflows/ci.yml"));

        Object loaded = new Yaml().load(source);

        assertThat(loaded).isInstanceOf(Map.class);
        Map<?, ?> workflow = (Map<?, ?>) loaded;
        assertThat(workflow.get("jobs")).isInstanceOf(Map.class);
        Map<?, ?> jobs = (Map<?, ?>) workflow.get("jobs");
        assertThat(jobs.containsKey("build")).isTrue();
        assertThat(jobs.containsKey("deploy-dev")).isTrue();
    }
}
