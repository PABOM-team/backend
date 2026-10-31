package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.*;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.careguideline.application.command.CareGuidelineImportFiles;
import com.pabom.backend.careguideline.domain.error.CareGuidelineErrorCode;
import com.pabom.backend.careguideline.domain.type.*;
import com.pabom.backend.careguideline.infrastructure.persistence.*;
import com.pabom.backend.careguideline.presentation.response.*;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-guideline-import-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Transactional
class CareGuidelineImportServiceIntegrationTest {
    private static final Path CSV_DIR = Path.of("src/docs/care-guidelines/v1_2");

    @Autowired CareGuidelineImportService service;
    @Autowired UserRepository users;
    @Autowired CareGuidelineVersionJpaRepository versions;
    @Autowired CareStandardJpaRepository standards;
    @Autowired CareStageThresholdJpaRepository thresholds;
    @Autowired CareItemDescriptionJpaRepository descriptions;
    @Autowired CareItemCodeMappingJpaRepository mappings;
    @Autowired CareItemJpaRepository items;

    @Test
    void dryRunValidatesAllFiveFilesWithoutChangingDatabase() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();

        CareGuidelineImportResponse response = service.importCsv(adminId, "1.2",
                CareGuidelineImportMode.DRY_RUN, false, null, files());

        assertThat(response.valid()).isTrue();
        assertThat(response.persisted()).isFalse();
        assertThat(response.counts()).isEqualTo(new CareGuidelineImportResponse.ImportCounts(3, 10, 46, 5, 14));
        assertThat(versions.count()).isZero();
        assertThat(standards.count()).isZero();
        assertThat(items.findByCode("FEED")).isEmpty();
        assertThat(mappings.findAll()).allMatch(mapping -> mapping.getGuidelineCareItemId() == null);
    }

    @Test
    void commitStoresAllDataAndLinksLegacyItemMappings() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();

        CareGuidelineImportResponse response = service.importCsv(adminId, "1.2",
                CareGuidelineImportMode.COMMIT, false, null, files());

        assertThat(response.persisted()).isTrue();
        assertThat(response.status()).isEqualTo(GuidelineVersionStatus.DRAFT);
        var version = versions.findByVersionLabel("1.2").orElseThrow();
        assertThat(standards.findAllByGuidelineVersionId(version.getId())).hasSize(46);
        assertThat(thresholds.findAllByGuidelineVersionId(version.getId())).hasSize(5);
        assertThat(descriptions.findAllByGuidelineVersionId(version.getId())).hasSize(14);
        assertThat(items.findByCode("FEED")).isPresent();
        assertThat(items.findByCode("SHED_CHECK")).isPresent();
        assertThat(items.findByCode("FEEDING")).isPresent();
        assertThat(items.findByCode("SHED")).isPresent();
        assertThat(mappings.findByGuidelineItemCode("FEED").orElseThrow().getGuidelineCareItemId()).isNotNull();
        assertThat(mappings.findByGuidelineItemCode("SHED_CHECK").orElseThrow().getGuidelineCareItemId()).isNotNull();
    }

    @Test
    void commitPublishesWithEffectiveDate() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();
        LocalDate effectiveFrom = LocalDate.of(2026, 11, 1);

        service.importCsv(adminId, "1.2", CareGuidelineImportMode.COMMIT,
                true, effectiveFrom, files());

        var version = versions.findByVersionLabel("1.2").orElseThrow();
        assertThat(version.getStatus()).isEqualTo(GuidelineVersionStatus.PUBLISHED);
        assertThat(version.getEffectiveFrom()).isEqualTo(effectiveFrom);
        assertThat(version.getPublishedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateVersion() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();
        service.importCsv(adminId, "1.2", CareGuidelineImportMode.COMMIT, false, null, files());

        assertThatThrownBy(() -> service.importCsv(adminId, "1.2", CareGuidelineImportMode.COMMIT,
                false, null, files()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CareGuidelineErrorCode.GUIDELINE_VERSION_DUPLICATED));
    }

    @Test
    void reportsHeaderAndRequiredValueLocationsAndDoesNotPersist() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();
        CareGuidelineImportFiles invalid = files();
        String stages = new String(invalid.stages().getBytes(), StandardCharsets.UTF_8)
                .replaceFirst("stage_code", "wrong_header");
        String items = new String(invalid.items().getBytes(), StandardCharsets.UTF_8)
                .replace("FEED,급여,FEED", "FEED,,FEED");
        invalid = new CareGuidelineImportFiles(csv("stages", stages), csv("items", items),
                invalid.standards(), invalid.stageThresholds(), invalid.descriptions());

        CareGuidelineImportFiles requestFiles = invalid;
        assertThatThrownBy(() -> service.importCsv(adminId, "1.2", CareGuidelineImportMode.COMMIT,
                false, null, requestFiles))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(CareGuidelineErrorCode.GUIDELINE_IMPORT_INVALID);
                    CareGuidelineImportErrorDetails details = (CareGuidelineImportErrorDetails) exception.details();
                    List<ImportValidationIssue> issues = details.errors();
                    assertThat(details.counts().standards()).isEqualTo(46);
                    assertThat(issues).anyMatch(issue -> issue.file().equals("stages.csv") && issue.row() == 1
                            && issue.code().equals("INVALID_HEADER"));
                    assertThat(issues).anyMatch(issue -> issue.file().equals("items.csv") && issue.row() == 2
                            && issue.column().equals("item_name") && issue.code().equals("REQUIRED_VALUE"));
                });
        assertThat(versions.count()).isZero();
    }

    @Test
    void reportsDuplicateAndReferenceErrorsAndRollsBackCommit() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();
        CareGuidelineImportFiles valid = files();
        String standardsText = new String(valid.standards().getBytes(), StandardCharsets.UTF_8);
        String firstDataRow = standardsText.lines().skip(1).findFirst().orElseThrow();
        standardsText = standardsText.replaceFirst("LEOPARD,JUVENILE,FEED", "NO_SPECIES,JUVENILE,FEED")
                + System.lineSeparator() + firstDataRow + System.lineSeparator() + firstDataRow;
        CareGuidelineImportFiles invalid = new CareGuidelineImportFiles(valid.stages(), valid.items(),
                csv("standards", standardsText), valid.stageThresholds(), valid.descriptions());

        assertThatThrownBy(() -> service.importCsv(adminId, "1.2", CareGuidelineImportMode.COMMIT,
                false, null, invalid))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    CareGuidelineImportErrorDetails details = (CareGuidelineImportErrorDetails) exception.details();
                    List<ImportValidationIssue> issues = details.errors();
                    assertThat(issues).anyMatch(issue -> issue.code().equals("REFERENCE_NOT_FOUND"));
                    assertThat(issues).anyMatch(issue -> issue.code().equals("DUPLICATED_ROW"));
                });
        assertThat(versions.count()).isZero();
        assertThat(standards.count()).isZero();
    }

    @Test
    void requiresAdminRole() throws Exception {
        Long userId = user(UserRole.USER).getId();

        assertThatThrownBy(() -> service.importCsv(userId, "1.2", CareGuidelineImportMode.DRY_RUN,
                false, null, files()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CareGuidelineErrorCode.ADMIN_REQUIRED));
    }

    @Test
    void rejectsOutOfRangeCycleAndUnknownStandard() throws Exception {
        Long adminId = user(UserRole.ADMIN).getId();
        CareGuidelineImportFiles valid = files();
        String standardsText = new String(valid.standards().getBytes(), StandardCharsets.UTF_8)
                .replaceFirst("LEOPARD,JUVENILE,FEED,1,1,2", "LEOPARD,UNKNOWN,FEED,9,1,2");
        CareGuidelineImportFiles invalid = new CareGuidelineImportFiles(valid.stages(), valid.items(),
                csv("standards", standardsText), valid.stageThresholds(), valid.descriptions());

        assertThatThrownBy(() -> service.importCsv(adminId, "1.2", CareGuidelineImportMode.DRY_RUN,
                false, null, invalid))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    CareGuidelineImportErrorDetails details = (CareGuidelineImportErrorDetails) exception.details();
                    List<ImportValidationIssue> issues = details.errors();
                    assertThat(issues).anyMatch(issue -> "stage_code".equals(issue.column())
                            && issue.message().contains("UNKNOWN"));
                    assertThat(issues).anyMatch(issue -> "default_cycle_days".equals(issue.column()));
                });
        assertThat(versions.count()).isZero();
    }

    private User user(UserRole role) {
        User user = User.pending(OAuthProvider.GOOGLE, "guideline-" + role + "-" + System.nanoTime(),
                "관리자", Instant.now());
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        return users.save(user);
    }

    private CareGuidelineImportFiles files() throws IOException {
        return new CareGuidelineImportFiles(
                file("stages", "pabom_v1_2_stages_FINAL.csv"),
                file("items", "pabom_v1_2_items_FINAL.csv"),
                file("standards", "pabom_v1_2_standards_FINAL.csv"),
                file("stageThresholds", "pabom_v1_2_stage_thresholds_FINAL.csv"),
                file("descriptions", "pabom_v1_2_descriptions_FINAL.csv"));
    }

    private MockMultipartFile file(String part, String name) throws IOException {
        return new MockMultipartFile(part, name, "text/csv", Files.readAllBytes(CSV_DIR.resolve(name)));
    }

    private MockMultipartFile csv(String part, String content) {
        return new MockMultipartFile(part, part + ".csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
