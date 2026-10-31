package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.careguideline.application.command.CareGuidelineImportFiles;
import com.pabom.backend.careguideline.domain.type.CareGuidelineImportMode;
import com.pabom.backend.careguideline.infrastructure.persistence.*;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.*;
import java.nio.file.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-guideline-rollback-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class CareGuidelineImportRollbackTest {
    private static final Path CSV_DIR = Path.of("src/docs/care-guidelines/v1_2");

    @Autowired CareGuidelineImportService service;
    @Autowired UserRepository users;
    @Autowired CareGuidelineVersionJpaRepository versions;
    @Autowired CareStandardJpaRepository standards;
    @Autowired CareItemCodeMappingJpaRepository mappings;
    @Autowired CareItemJpaRepository items;
    @MockitoSpyBean CareItemDescriptionJpaRepository descriptions;

    @Test
    void rollsBackEveryTableWhenPersistenceFailsAfterValidation() throws Exception {
        User admin = User.pending(OAuthProvider.GOOGLE, "guideline-rollback-admin", "관리자", Instant.now());
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "status", UserStatus.ACTIVE);
        admin = users.save(admin);
        doThrow(new DataAccessResourceFailureException("simulated database failure"))
                .when(descriptions).saveAll(any());

        Long adminId = admin.getId();
        assertThatThrownBy(() -> service.importCsv(adminId, "1.2", CareGuidelineImportMode.COMMIT,
                false, null, files())).isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(versions.count()).isZero();
        assertThat(standards.count()).isZero();
        assertThat(items.findByCode("FEED")).isEmpty();
        assertThat(mappings.findAll()).allMatch(mapping -> mapping.getGuidelineCareItemId() == null);
    }

    private CareGuidelineImportFiles files() throws Exception {
        return new CareGuidelineImportFiles(
                file("stages", "pabom_v1_2_stages_FINAL.csv"),
                file("items", "pabom_v1_2_items_FINAL.csv"),
                file("standards", "pabom_v1_2_standards_FINAL.csv"),
                file("stageThresholds", "pabom_v1_2_stage_thresholds_FINAL.csv"),
                file("descriptions", "pabom_v1_2_descriptions_FINAL.csv"));
    }

    private MockMultipartFile file(String part, String name) throws Exception {
        return new MockMultipartFile(part, name, "text/csv", Files.readAllBytes(CSV_DIR.resolve(name)));
    }
}
