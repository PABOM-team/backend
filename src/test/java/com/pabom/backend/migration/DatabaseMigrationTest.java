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
    private static final String FILE_VERSION = "20261017000100";
    private static final String PET_VERSION = "20261018000100";
    private static final String ADOPTED_AT_VERSION = "20261018000200";
    private static final String RECORD_VERSION = "20261020000100";

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
        Boolean fileMigrationSuccess = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = ?",
                Boolean.class,
                FILE_VERSION
        );
        Boolean petMigrationSuccess = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = ?",
                Boolean.class,
                PET_VERSION
        );
        Boolean adoptedAtMigrationSuccess = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = ?",
                Boolean.class,
                ADOPTED_AT_VERSION
        );
        Boolean recordMigrationSuccess = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = ?",
                Boolean.class,
                RECORD_VERSION
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
        assertThat(fileMigrationSuccess).isTrue();
        assertThat(petMigrationSuccess).isTrue();
        assertThat(adoptedAtMigrationSuccess).isTrue();
        assertThat(recordMigrationSuccess).isTrue();
        assertThat(tables).contains(
                "users",
                "refresh_tokens",
                "user_agreements",
                "events",
                "files",
                "species",
                "morphs",
                "life_stages",
                "pets",
                "pet_morphs",
                "care_items",
                "routines",
                "care_records",
                "care_record_revisions"
        );
    }

    @Test
    void addsNullableAdoptedAtDateColumn() {
        var column = jdbcTemplate.queryForMap(
                """
                SELECT data_type, is_nullable
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = 'pets'
                   AND column_name = 'adopted_at'
                """
        );

        assertThat(column.get("DATA_TYPE").toString()).isEqualToIgnoringCase("date");
        assertThat(column.get("IS_NULLABLE").toString()).isEqualToIgnoringCase("yes");
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
                "fk_events_user",
                "pk_species",
                "uk_species_code",
                "pk_morphs",
                "uk_morphs_species_code",
                "fk_morphs_species",
                "pk_pets",
                "uk_pets_user_client_request",
                "fk_pets_user",
                "fk_pets_species",
                "pk_pet_morphs",
                "fk_pet_morphs_pet",
                "fk_pet_morphs_morph"
                ,"pk_care_items",
                "uk_care_items_code",
                "pk_routines",
                "pk_care_records",
                "uk_care_records_user_client_request",
                "pk_care_record_revisions",
                "fk_care_record_revisions_record"
        );
        assertThat(indexes).contains(
                "idx_refresh_tokens_family_id",
                "idx_refresh_tokens_user_id",
                "idx_user_agreements_user_id",
                "idx_events_user_id",
                "idx_morphs_species_id",
                "idx_pets_user_created_at",
                "idx_pet_morphs_morph_id"
                ,"idx_routines_pet_active",
                "idx_care_records_user_occurred",
                "idx_care_record_revisions_record"
        );
    }

    @Test
    void seedsSpeciesMorphsAndLifeStagesExactlyOnce() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM species", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM morphs", Integer.class)).isEqualTo(17);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM life_stages", Integer.class)).isEqualTo(3);

        flyway.migrate();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM species", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM morphs", Integer.class)).isEqualTo(17);
    }

    @Test
    void doesNotRunVersionedMigrationTwice() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void seedsOnlyRecordItemMetadataWithoutInventingGuidelineRanges() {
        assertThat(jdbcTemplate.queryForList(
                "select code from care_items order by id", String.class))
                .containsExactly("FEEDING", "WEIGHT", "SHED", "MEMO");
        var weight = jdbcTemplate.queryForMap(
                "select unit,min_value,max_value from care_items where code='WEIGHT'");
        assertThat(weight.get("UNIT")).isEqualTo("g");
        assertThat(weight.get("MIN_VALUE")).isNull();
        assertThat(weight.get("MAX_VALUE")).isNull();
    }
}
