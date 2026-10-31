package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.application.command.CareGuidelineImportFiles;
import com.pabom.backend.careguideline.application.csv.*;
import com.pabom.backend.careguideline.domain.entity.*;
import com.pabom.backend.careguideline.domain.error.CareGuidelineErrorCode;
import com.pabom.backend.careguideline.domain.type.*;
import com.pabom.backend.careguideline.infrastructure.persistence.*;
import com.pabom.backend.careguideline.presentation.response.*;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.type.CareItemInputType;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.species.domain.entity.*;
import com.pabom.backend.species.infrastructure.persistence.*;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CareGuidelineImportService {
    private static final List<String> STAGE_HEADERS = List.of(
            "stage_code", "stage_name", "display_order", "has_own_guideline_rules", "guideline_fallback_stage");
    private static final List<String> ITEM_HEADERS = List.of(
            "item_code", "item_name", "record_type", "unit", "active", "suggested_existing_input_type",
            "outcome_allowed", "requires_input_model_extension");
    private static final List<String> STANDARD_HEADERS = List.of(
            "species_code", "stage_code", "item_code", "default_cycle_days", "min_days", "max_days",
            "record_type", "feed_result_set", "default_food_name", "default_enabled", "evidence_level",
            "evidence_source_label", "source_refs", "guideline_version");
    private static final List<String> THRESHOLD_HEADERS = List.of(
            "species_code", "sex", "ref_weight_g", "min_age_months", "hatch_adult_age_months", "guideline_version");
    private static final List<String> DESCRIPTION_HEADERS = List.of(
            "description_key", "description_text", "guideline_version");
    private static final Set<String> STAGE_CODES = Set.of("JUVENILE", "ADULT", "UNKNOWN");
    private static final Set<String> RECORD_TYPES = Set.of("FEED", "OX", "WEIGHT");
    private static final Set<String> EVIDENCE_LEVELS = Set.of("VET_SOURCE", "TEXTBOOK", "TEAM_JUDGMENT");
    private static final Set<String> FEED_RESULT_SETS = Set.of("INSECT", "DIET", "WHOLE_PREY");
    private static final Set<String> THRESHOLD_SEXES = Set.of("ALL", "MALE", "FEMALE", "UNKNOWN");

    private final Utf8CsvParser parser;
    private final UserRepository users;
    private final SpeciesJpaRepository speciesRepository;
    private final LifeStageJpaRepository stageRepository;
    private final CareItemJpaRepository itemRepository;
    private final CareGuidelineVersionJpaRepository versionRepository;
    private final CareItemCodeMappingJpaRepository itemCodeMappingRepository;
    private final CareStandardJpaRepository standardRepository;
    private final CareStageThresholdJpaRepository thresholdRepository;
    private final CareItemDescriptionJpaRepository descriptionRepository;
    private final Clock clock;

    @Transactional
    public CareGuidelineImportResponse importCsv(Long userId, String versionLabel,
            CareGuidelineImportMode mode, boolean publish, LocalDate effectiveFrom,
            CareGuidelineImportFiles files) {
        requireAdmin(userId);
        String normalizedVersion = versionLabel;
        if (normalizedVersion != null && versionRepository.existsByVersionLabel(normalizedVersion)) {
            throw new BusinessException(CareGuidelineErrorCode.GUIDELINE_VERSION_DUPLICATED);
        }

        List<ImportValidationIssue> issues = new ArrayList<>();
        if (normalizedVersion == null || normalizedVersion.isEmpty() || normalizedVersion.length() > 100) {
            issues.add(issue("request", null, "versionLabel", "INVALID_VALUE", "versionLabel은 1~100자여야 합니다."));
        } else if (!normalizedVersion.equals(normalizedVersion.trim())) {
            issues.add(issue("request", null, "versionLabel", "INVALID_VALUE", "versionLabel 앞뒤에 공백을 사용할 수 없습니다."));
        }
        if (mode == null) issues.add(issue("request", null, "mode", "REQUIRED_VALUE", "mode는 필수입니다."));
        if (publish && effectiveFrom == null) {
            issues.add(issue("request", null, "effectiveFrom", "REQUIRED_VALUE", "게시할 때 effectiveFrom은 필수입니다."));
        }

        CsvTable stagesTable = parser.parse("stages", files.stages(), STAGE_HEADERS, issues);
        CsvTable itemsTable = parser.parse("items", files.items(), ITEM_HEADERS, issues);
        CsvTable standardsTable = parser.parse("standards", files.standards(), STANDARD_HEADERS, issues);
        CsvTable thresholdsTable = parser.parse("stageThresholds", files.stageThresholds(), THRESHOLD_HEADERS, issues);
        CsvTable descriptionsTable = parser.parse("descriptions", files.descriptions(), DESCRIPTION_HEADERS, issues);

        Map<String, Species> species = speciesRepository.findAll().stream()
                .collect(Collectors.toMap(Species::getCode, Function.identity()));
        List<StageRow> stages = validateStages(stagesTable, issues);
        List<ItemRow> items = validateItems(itemsTable, issues);
        Map<String, StageRow> stageRows = uniqueMap(stages, StageRow::code);
        Map<String, ItemRow> itemRows = uniqueMap(items, ItemRow::code);
        validateCompatibilityItems(itemRows, issues);
        List<StandardRow> standards = validateStandards(
                standardsTable, normalizedVersion, species, stageRows, itemRows, issues);
        List<ThresholdRow> thresholds = validateThresholds(
                thresholdsTable, normalizedVersion, species, issues);
        List<DescriptionRow> descriptions = validateDescriptions(
                descriptionsTable, normalizedVersion, species, itemRows, issues);

        CareGuidelineImportResponse.ImportCounts counts = new CareGuidelineImportResponse.ImportCounts(
                stages.size(), items.size(), standards.size(), thresholds.size(), descriptions.size());
        if (!issues.isEmpty()) {
            throw new BusinessException(CareGuidelineErrorCode.GUIDELINE_IMPORT_INVALID,
                    new CareGuidelineImportErrorDetails(counts, withOriginalFileNames(issues, files)));
        }

        GuidelineVersionStatus status = publish ? GuidelineVersionStatus.PUBLISHED : GuidelineVersionStatus.DRAFT;
        if (mode == CareGuidelineImportMode.DRY_RUN) {
            return new CareGuidelineImportResponse(normalizedVersion, mode, true, false, status, counts);
        }

        Instant now = clock.instant();
        CareGuidelineVersion version = versionRepository.saveAndFlush(CareGuidelineVersion.create(
                normalizedVersion, publish, effectiveFrom, userId, now));
        persistStages(stages);
        persistItems(items);
        Map<String, LifeStage> persistedStages = stageRepository.findAll().stream()
                .collect(Collectors.toMap(LifeStage::getCode, Function.identity()));
        Map<String, CareItem> persistedItems = itemRepository.findAll().stream()
                .collect(Collectors.toMap(CareItem::getCode, Function.identity()));
        linkCompatibilityMappings(persistedItems);
        standardRepository.saveAll(standards.stream().map(row -> CareStandard.of(
                version.getId(), species.get(row.speciesCode()).getId(), persistedStages.get(row.stageCode()).getId(),
                persistedItems.get(row.itemCode()).getId(), row.defaultCycleDays(), row.minDays(), row.maxDays(),
                row.recordType(), row.feedResultSet(), row.defaultFoodName(), row.defaultEnabled(),
                row.evidenceLevel(), row.evidenceSourceLabel(), row.sourceRefs())).toList());
        thresholdRepository.saveAll(thresholds.stream().map(row -> CareStageThreshold.of(
                version.getId(), species.get(row.speciesCode()).getId(), row.sex(), row.refWeightG(),
                row.minAgeMonths(), row.hatchAdultAgeMonths())).toList());
        descriptionRepository.saveAll(descriptions.stream().map(row -> CareItemDescription.of(
                version.getId(), row.descriptionKey(),
                row.speciesCode() == null ? null : species.get(row.speciesCode()).getId(),
                persistedItems.get(row.itemCode()).getId(), row.descriptionText())).toList());
        return new CareGuidelineImportResponse(normalizedVersion, mode, true, true, status, counts);
    }

    private void requireAdmin(Long userId) {
        User user = users.findById(userId).orElseThrow(() ->
                new BusinessException(CareGuidelineErrorCode.ADMIN_REQUIRED));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(AuthErrorCode.USER_WITHDRAWN);
        }
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(CareGuidelineErrorCode.ADMIN_REQUIRED);
        }
    }

    private List<StageRow> validateStages(CsvTable table, List<ImportValidationIssue> issues) {
        List<StageRow> result = new ArrayList<>(); Set<String> keys = new HashSet<>();
        for (CsvTable.Row row : table.rows()) {
            String code = required("stages", row, "stage_code", issues);
            String name = required("stages", row, "stage_name", issues);
            Integer order = positiveInt("stages", row, "display_order", issues);
            Boolean own = bool("stages", row, "has_own_guideline_rules", issues);
            String fallback = blankToNull(row.get("guideline_fallback_stage"));
            duplicate("stages", row, "stage_code", code, keys, issues);
            if (code != null && !STAGE_CODES.contains(code)) invalid("stages", row, "stage_code", "지원하지 않는 성장 단계 코드입니다.", issues);
            if (Boolean.FALSE.equals(own) && fallback == null) invalid("stages", row, "guideline_fallback_stage", "자체 규칙이 없으면 fallback 단계가 필요합니다.", issues);
            if (Boolean.TRUE.equals(own) && fallback != null) invalid("stages", row, "guideline_fallback_stage", "자체 규칙이 있는 단계에는 fallback을 지정할 수 없습니다.", issues);
            if (code != null && name != null && order != null && own != null) result.add(new StageRow(code, name, order, own, fallback, row.number()));
        }
        Set<String> found = result.stream().map(StageRow::code).collect(Collectors.toSet());
        for (String required : STAGE_CODES) if (!found.contains(required))
            issues.add(issue("stages", null, "stage_code", "REQUIRED_ROW_MISSING", "필수 성장 단계가 없습니다: " + required));
        Map<String, StageRow> byCode = uniqueMap(result, StageRow::code);
        for (StageRow row : result) if (row.fallback() != null && !byCode.containsKey(row.fallback()))
            issues.add(issue("stages", row.row(), "guideline_fallback_stage", "REFERENCE_NOT_FOUND", "fallback 성장 단계가 없습니다: " + row.fallback()));
        StageRow unknown = byCode.get("UNKNOWN");
        if (unknown != null && (unknown.ownRules() || !"JUVENILE".equals(unknown.fallback())))
            issues.add(issue("stages", unknown.row(), "guideline_fallback_stage", "INVALID_UNKNOWN_POLICY", "UNKNOWN은 JUVENILE fallback이어야 합니다."));
        return result;
    }

    private List<ItemRow> validateItems(CsvTable table, List<ImportValidationIssue> issues) {
        List<ItemRow> result = new ArrayList<>(); Set<String> keys = new HashSet<>();
        for (CsvTable.Row row : table.rows()) {
            String code = required("items", row, "item_code", issues);
            String name = required("items", row, "item_name", issues);
            String recordType = enumValue("items", row, "record_type", RECORD_TYPES, issues);
            String unit = blankToNull(row.get("unit"));
            Boolean active = bool("items", row, "active", issues);
            String inputRaw = required("items", row, "suggested_existing_input_type", issues);
            CareItemInputType inputType = null;
            if (inputRaw != null) try { inputType = CareItemInputType.valueOf(inputRaw); }
            catch (IllegalArgumentException e) { invalid("items", row, "suggested_existing_input_type", "지원하지 않는 입력 방식입니다.", issues); }
            Boolean outcomeAllowed = bool("items", row, "outcome_allowed", issues);
            Boolean extension = bool("items", row, "requires_input_model_extension", issues);
            duplicate("items", row, "item_code", code, keys, issues);
            if (code != null && name != null && recordType != null && active != null && inputType != null
                    && outcomeAllowed != null && extension != null)
                result.add(new ItemRow(code, name, recordType, unit, active, inputType, outcomeAllowed, extension, row.number()));
        }
        return result;
    }

    private List<StandardRow> validateStandards(CsvTable table, String version,
            Map<String, Species> species, Map<String, StageRow> stages, Map<String, ItemRow> items,
            List<ImportValidationIssue> issues) {
        List<StandardRow> result = new ArrayList<>(); Set<String> keys = new HashSet<>();
        for (CsvTable.Row row : table.rows()) {
            String speciesCode = required("standards", row, "species_code", issues);
            String stageCode = required("standards", row, "stage_code", issues);
            String itemCode = required("standards", row, "item_code", issues);
            Integer cycle = positiveInt("standards", row, "default_cycle_days", issues);
            Integer min = positiveInt("standards", row, "min_days", issues);
            Integer max = positiveInt("standards", row, "max_days", issues);
            String recordType = enumValue("standards", row, "record_type", RECORD_TYPES, issues);
            String feedSet = blankToNull(row.get("feed_result_set"));
            String food = blankToNull(row.get("default_food_name"));
            Boolean enabled = bool("standards", row, "default_enabled", issues);
            String evidence = enumValue("standards", row, "evidence_level", EVIDENCE_LEVELS, issues);
            String sourceLabel = required("standards", row, "evidence_source_label", issues);
            String sourceRefs = required("standards", row, "source_refs", issues);
            version("standards", row, version, issues);
            String key = speciesCode + "|" + stageCode + "|" + itemCode;
            duplicate("standards", row, "item_code", key, keys, issues);
            reference("standards", row, "species_code", speciesCode, species, issues);
            reference("standards", row, "stage_code", stageCode, stages, issues);
            reference("standards", row, "item_code", itemCode, items, issues);
            if ("UNKNOWN".equals(stageCode)) invalid("standards", row, "stage_code", "UNKNOWN에는 별도 기준을 등록할 수 없습니다.", issues);
            StageRow stage = stages.get(stageCode);
            if (stage != null && !stage.ownRules()) invalid("standards", row, "stage_code", "자체 규칙이 없는 단계입니다.", issues);
            ItemRow item = items.get(itemCode);
            if (item != null && recordType != null && !item.recordType().equals(recordType))
                invalid("standards", row, "record_type", "items.csv의 record_type과 일치하지 않습니다.", issues);
            if (cycle != null && min != null && max != null && !(min <= cycle && cycle <= max))
                invalid("standards", row, "default_cycle_days", "min_days <= default_cycle_days <= max_days여야 합니다.", issues);
            if ("FEED".equals(recordType)) {
                if (feedSet == null || !FEED_RESULT_SETS.contains(feedSet)) invalid("standards", row, "feed_result_set", "FEED 기준에는 유효한 feed_result_set이 필요합니다.", issues);
                if (food == null) invalid("standards", row, "default_food_name", "FEED 기준에는 default_food_name이 필요합니다.", issues);
            } else if (feedSet != null || food != null) {
                invalid("standards", row, "feed_result_set", "FEED가 아닌 기준에는 급여 기본값을 지정할 수 없습니다.", issues);
            }
            if (speciesCode != null && stageCode != null && itemCode != null && cycle != null && min != null && max != null
                    && recordType != null && enabled != null && evidence != null && sourceLabel != null && sourceRefs != null)
                result.add(new StandardRow(speciesCode, stageCode, itemCode, cycle, min, max, recordType,
                        feedSet, food, enabled, evidence, sourceLabel, sourceRefs));
        }
        return result;
    }

    private List<ThresholdRow> validateThresholds(CsvTable table, String version,
            Map<String, Species> species, List<ImportValidationIssue> issues) {
        List<ThresholdRow> result = new ArrayList<>(); Set<String> keys = new HashSet<>();
        for (CsvTable.Row row : table.rows()) {
            String speciesCode = required("stageThresholds", row, "species_code", issues);
            String sex = enumValue("stageThresholds", row, "sex", THRESHOLD_SEXES, issues);
            BigDecimal weight = positiveDecimal("stageThresholds", row, "ref_weight_g", issues);
            Integer minAge = positiveInt("stageThresholds", row, "min_age_months", issues);
            Integer hatchAge = positiveInt("stageThresholds", row, "hatch_adult_age_months", issues);
            version("stageThresholds", row, version, issues);
            duplicate("stageThresholds", row, "sex", speciesCode + "|" + sex, keys, issues);
            reference("stageThresholds", row, "species_code", speciesCode, species, issues);
            if (minAge != null && hatchAge != null && minAge > hatchAge)
                invalid("stageThresholds", row, "min_age_months", "min_age_months는 hatch_adult_age_months 이하여야 합니다.", issues);
            if (speciesCode != null && sex != null && weight != null && minAge != null && hatchAge != null)
                result.add(new ThresholdRow(speciesCode, sex, weight, minAge, hatchAge));
        }
        return result;
    }

    private List<DescriptionRow> validateDescriptions(CsvTable table, String version,
            Map<String, Species> species, Map<String, ItemRow> items, List<ImportValidationIssue> issues) {
        List<DescriptionRow> result = new ArrayList<>(); Set<String> keys = new HashSet<>();
        Map<String, String> speciesKeys = new HashMap<>();
        species.keySet().forEach(code -> speciesKeys.put(code.toLowerCase(Locale.ROOT).replace("_", ""), code));
        for (CsvTable.Row row : table.rows()) {
            String key = required("descriptions", row, "description_key", issues);
            String text = required("descriptions", row, "description_text", issues);
            version("descriptions", row, version, issues);
            duplicate("descriptions", row, "description_key", key, keys, issues);
            String speciesCode = null; String itemCode = null;
            if (key != null) {
                String[] parts = key.split("\\.", -1);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    invalid("descriptions", row, "description_key", "description_key는 {species|*}.{item_code} 형식이어야 합니다.", issues);
                } else {
                    itemCode = parts[1];
                    if (!"*".equals(parts[0])) {
                        speciesCode = speciesKeys.get(parts[0].toLowerCase(Locale.ROOT).replace("_", ""));
                        if (speciesCode == null) invalid("descriptions", row, "description_key", "등록되지 않은 종 식별자입니다: " + parts[0], issues);
                    }
                    reference("descriptions", row, "description_key", itemCode, items, issues);
                }
            }
            if (key != null && text != null && itemCode != null && items.containsKey(itemCode)
                    && (key.startsWith("*.") || speciesCode != null))
                result.add(new DescriptionRow(key, speciesCode, itemCode, text));
        }
        return result;
    }

    private void persistStages(List<StageRow> rows) {
        Map<String, LifeStage> stages = stageRepository.findAll().stream()
                .collect(Collectors.toMap(LifeStage::getCode, Function.identity()));
        for (StageRow row : rows) {
            LifeStage stage = stages.get(row.code());
            Long fallbackId = row.fallback() == null ? null : stages.get(row.fallback()).getId();
            stage.updateGuidelineMetadata(row.name(), row.displayOrder(), row.ownRules(), fallbackId);
        }
        stageRepository.saveAllAndFlush(stages.values());
    }

    private void persistItems(List<ItemRow> rows) {
        Map<String, CareItem> existing = itemRepository.findAll().stream()
                .collect(Collectors.toMap(CareItem::getCode, Function.identity()));
        List<CareItem> saved = new ArrayList<>();
        for (ItemRow row : rows) {
            CareItem item = existing.get(row.code());
            if (item == null) item = CareItem.guidelineItem(row.code(), row.name(), row.inputType(), row.unit(),
                    row.outcomeAllowed(), row.active(), row.recordType(), row.extension());
            else item.updateGuidelineMetadata(row.name(), row.inputType(), row.unit(), row.outcomeAllowed(),
                    row.active(), row.recordType(), row.extension());
            saved.add(item);
        }
        itemRepository.saveAllAndFlush(saved);
    }

    private void validateCompatibilityItems(Map<String, ItemRow> items, List<ImportValidationIssue> issues) {
        for (CareItemCodeMapping mapping : itemCodeMappingRepository.findAll()) {
            if (!items.containsKey(mapping.getGuidelineItemCode())) {
                issues.add(issue("items", null, "item_code", "REQUIRED_ROW_MISSING",
                        "기존 코드 호환성 매핑에 필요한 항목이 없습니다: " + mapping.getGuidelineItemCode()));
            }
        }
    }

    private void linkCompatibilityMappings(Map<String, CareItem> persistedItems) {
        List<CareItemCodeMapping> mappings = itemCodeMappingRepository.findAll();
        for (CareItemCodeMapping mapping : mappings) {
            CareItem guidelineItem = persistedItems.get(mapping.getGuidelineItemCode());
            if (guidelineItem != null) mapping.linkGuidelineItem(guidelineItem.getId());
        }
        itemCodeMappingRepository.saveAll(mappings);
    }

    private List<ImportValidationIssue> withOriginalFileNames(List<ImportValidationIssue> issues,
            CareGuidelineImportFiles files) {
        Map<String, String> names = Map.of(
                "stages", originalName(files.stages(), "stages.csv"),
                "items", originalName(files.items(), "items.csv"),
                "standards", originalName(files.standards(), "standards.csv"),
                "stageThresholds", originalName(files.stageThresholds(), "stage_thresholds.csv"),
                "descriptions", originalName(files.descriptions(), "descriptions.csv"));
        return issues.stream().map(issue -> new ImportValidationIssue(
                names.getOrDefault(issue.file(), issue.file()), issue.row(), issue.column(), issue.code(), issue.message()))
                .toList();
    }

    private String originalName(org.springframework.web.multipart.MultipartFile file, String fallback) {
        return file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? fallback : file.getOriginalFilename();
    }

    private String required(String file, CsvTable.Row row, String column, List<ImportValidationIssue> issues) {
        String value = blankToNull(row.get(column));
        if (value == null) issues.add(issue(file, row.number(), column, "REQUIRED_VALUE", "필수값입니다."));
        return value;
    }
    private Boolean bool(String file, CsvTable.Row row, String column, List<ImportValidationIssue> issues) {
        String value = required(file, row, column, issues); if (value == null) return null;
        if ("true".equalsIgnoreCase(value)) return true; if ("false".equalsIgnoreCase(value)) return false;
        invalid(file, row, column, "true 또는 false여야 합니다.", issues); return null;
    }
    private Integer positiveInt(String file, CsvTable.Row row, String column, List<ImportValidationIssue> issues) {
        String value = required(file, row, column, issues); if (value == null) return null;
        try { int parsed = Integer.parseInt(value); if (parsed > 0) return parsed; }
        catch (NumberFormatException ignored) {}
        invalid(file, row, column, "1 이상의 정수여야 합니다.", issues); return null;
    }
    private BigDecimal positiveDecimal(String file, CsvTable.Row row, String column, List<ImportValidationIssue> issues) {
        String value = required(file, row, column, issues); if (value == null) return null;
        try { BigDecimal parsed = new BigDecimal(value); if (parsed.signum() > 0) return parsed; }
        catch (NumberFormatException ignored) {}
        invalid(file, row, column, "0보다 큰 숫자여야 합니다.", issues); return null;
    }
    private String enumValue(String file, CsvTable.Row row, String column, Set<String> allowed,
            List<ImportValidationIssue> issues) {
        String value = required(file, row, column, issues); if (value == null) return null;
        if (!allowed.contains(value)) { invalid(file, row, column, "허용값은 " + allowed + "입니다.", issues); return null; }
        return value;
    }
    private void version(String file, CsvTable.Row row, String expected, List<ImportValidationIssue> issues) {
        String actual = required(file, row, "guideline_version", issues);
        if (actual != null && expected != null && !actual.equals(expected))
            invalid(file, row, "guideline_version", "요청 versionLabel과 일치해야 합니다.", issues);
    }
    private <T> void reference(String file, CsvTable.Row row, String column, String key,
            Map<String, T> values, List<ImportValidationIssue> issues) {
        if (key != null && !values.containsKey(key)) invalid(file, row, column, "참조 코드를 찾을 수 없습니다: " + key, issues, "REFERENCE_NOT_FOUND");
    }
    private void duplicate(String file, CsvTable.Row row, String column, String key,
            Set<String> keys, List<ImportValidationIssue> issues) {
        if (key != null && !keys.add(key)) invalid(file, row, column, "중복된 값입니다: " + key, issues, "DUPLICATED_ROW");
    }
    private void invalid(String file, CsvTable.Row row, String column, String message, List<ImportValidationIssue> issues) {
        invalid(file, row, column, message, issues, "INVALID_VALUE");
    }
    private void invalid(String file, CsvTable.Row row, String column, String message,
            List<ImportValidationIssue> issues, String code) {
        issues.add(issue(file, row.number(), column, code, message));
    }
    private ImportValidationIssue issue(String file, Integer row, String column, String code, String message) {
        return new ImportValidationIssue(file, row, column, code, message);
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private <T> Map<String, T> uniqueMap(List<T> values, Function<T, String> key) {
        return values.stream().collect(Collectors.toMap(key, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private record StageRow(String code, String name, int displayOrder, boolean ownRules, String fallback, int row) {}
    private record ItemRow(String code, String name, String recordType, String unit, boolean active,
            CareItemInputType inputType, boolean outcomeAllowed, boolean extension, int row) {}
    private record StandardRow(String speciesCode, String stageCode, String itemCode, int defaultCycleDays,
            int minDays, int maxDays, String recordType, String feedResultSet, String defaultFoodName,
            boolean defaultEnabled, String evidenceLevel, String evidenceSourceLabel, String sourceRefs) {}
    private record ThresholdRow(String speciesCode, String sex, BigDecimal refWeightG,
            int minAgeMonths, int hatchAdultAgeMonths) {}
    private record DescriptionRow(String descriptionKey, String speciesCode, String itemCode, String descriptionText) {}
}
