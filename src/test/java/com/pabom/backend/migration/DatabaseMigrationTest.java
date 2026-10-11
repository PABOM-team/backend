package com.pabom.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-migration-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class DatabaseMigrationTest {

    private static final String INITIAL_VERSION = "20261011000100";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void appliesAllMigrationsToAnEmptyDatabase() {
        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = ?",
                Boolean.class,
                INITIAL_VERSION
        );
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                """,
                String.class
        );

        assertThat(success).isTrue();
        assertThat(tables).contains(
                "users",
                "refresh_tokens",
                "user_agreements",
                "events"
        );
    }

    @Test
    void createsDeclaredConstraintsAndIndexes() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                  FROM information_schema.table_constraints
                 WHERE table_schema = 'public'
                """,
                String.class
        );
        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT index_name
                  FROM information_schema.indexes
                 WHERE table_schema = 'public'
                """,
                String.class
        );

        assertThat(constraints).contains(
                "pk_users",
                "uk_users_provider_provider_id",
                "pk_refresh_tokens",
                "uk_refresh_tokens_token_hash",
                "fk_refresh_tokens_user",
                "pk_user_agreements",
                "fk_user_agreements_user",
                "pk_events",
                "fk_events_user"
        );
        assertThat(indexes).contains(
                "idx_refresh_tokens_family_id",
                "idx_refresh_tokens_user_id",
                "idx_user_agreements_user_id",
                "idx_events_user_id"
        );
    }

    @Test
    void doesNotRunVersionedMigrationTwice() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
