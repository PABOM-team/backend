package com.pabom.backend.pet.presentation.docs;

public final class PetOpenApiExamples {

    private PetOpenApiExamples() {
    }

    public static final String CREATE_REQUEST = """
            {"clientRequestId":"550e8400-e29b-41d4-a716-446655440000","speciesId":1,"name":"콩이","morphIds":[2,4],"customMorph":null,"hatchedAt":"2026-03-02","lifeStageId":1,"stageCertainty":"CONFIRMED","sex":"FEMALE","sexCertainty":"ESTIMATED","photoFileId":null}
            """;

    public static final String UPDATE_REQUEST = """
            {"name":"콩콩이","morphIds":[3],"customMorph":null,"hatchedAt":null,"sex":"UNKNOWN","sexCertainty":"UNKNOWN","photoFileId":null}
            """;

    public static final String CREATE_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"id":12,"speciesId":1,"name":"콩이","morphs":[{"id":2,"code":"HARLEQUIN","name":"할리퀸"},{"id":4,"code":"DALMATIAN","name":"달마시안"}],"customMorph":null,"hatchedAt":"2026-03-02","lifeStageId":1,"stageCertainty":"CONFIRMED","sex":"FEMALE","sexCertainty":"ESTIMATED","photoFileId":null,"createdAt":"2026-10-18T07:52:18.222Z","updatedAt":"2026-10-18T07:52:18.222Z"}},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;

    public static final String LIST_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":[{"id":12,"speciesId":1,"name":"콩이","morphs":[{"id":2,"code":"HARLEQUIN","name":"할리퀸"}],"customMorph":null,"hatchedAt":"2026-03-02","lifeStageId":1,"stageCertainty":"CONFIRMED","sex":"FEMALE","sexCertainty":"ESTIMATED","photoFileId":null,"createdAt":"2026-10-18T07:52:18.222Z","updatedAt":"2026-10-18T07:52:18.222Z"}]},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;

    public static final String DETAIL_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"id":12,"speciesId":1,"name":"콩이","morphs":[{"id":2,"code":"HARLEQUIN","name":"할리퀸"}],"customMorph":null,"hatchedAt":"2026-03-02","lifeStageId":1,"stageCertainty":"CONFIRMED","sex":"FEMALE","sexCertainty":"ESTIMATED","photoFileId":null,"createdAt":"2026-10-18T07:52:18.222Z","updatedAt":"2026-10-18T07:52:18.222Z"}},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets/12"}}
            """;

    public static final String UPDATE_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"id":12,"speciesId":1,"name":"콩콩이","morphs":[{"id":3,"code":"FLAME","name":"플레임"}],"customMorph":null,"hatchedAt":null,"lifeStageId":1,"stageCertainty":"CONFIRMED","sex":"UNKNOWN","sexCertainty":"UNKNOWN","photoFileId":null,"createdAt":"2026-10-18T07:52:18.222Z","updatedAt":"2026-10-18T08:10:00.000Z"}},"error":null,"meta":{"timestamp":"2026-10-18T08:10:00.000","path":"/api/v1/pets/12"}}
            """;

    public static final String VALIDATION_FAILED = """
            {"resultType":"FAIL","success":null,"error":{"code":"VALIDATION_FAILED","message":"요청값이 누락되었거나 올바르지 않습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String INVALID_TOKEN = """
            {"resultType":"FAIL","success":null,"error":{"code":"INVALID_TOKEN","message":"유효하지 않거나 만료된 인증 토큰입니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String SPECIES_NOT_FOUND = """
            {"resultType":"FAIL","success":null,"error":{"code":"SPECIES_NOT_FOUND","message":"종을 찾을 수 없습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String PET_NOT_FOUND = """
            {"resultType":"FAIL","success":null,"error":{"code":"PET_NOT_FOUND","message":"개체를 찾을 수 없습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets/999"}}
            """;
    public static final String LIFE_STAGE_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"LIFE_STAGE_INVALID","message":"유효하지 않은 성장 단계입니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String MORPH_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"MORPH_INVALID","message":"해당 종에서 선택할 수 없는 모프입니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String MORPH_DUPLICATED = """
            {"resultType":"FAIL","success":null,"error":{"code":"MORPH_DUPLICATED","message":"동일한 모프를 중복 선택할 수 없습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String MORPH_COMBINATION_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"MORPH_COMBINATION_INVALID","message":"노멀 모프는 단독으로만 선택할 수 있습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String CUSTOM_MORPH_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"CUSTOM_MORPH_INVALID","message":"기타 모프는 40자 이하여야 합니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String PET_NAME_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"PET_NAME_INVALID","message":"개체 이름은 1자 이상 20자 이하여야 합니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String HATCHED_AT_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"HATCHED_AT_INVALID","message":"부화일은 미래일 수 없습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
    public static final String PET_PHOTO_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"PET_PHOTO_INVALID","message":"사용할 수 없는 개체 사진 파일입니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/pets"}}
            """;
}
