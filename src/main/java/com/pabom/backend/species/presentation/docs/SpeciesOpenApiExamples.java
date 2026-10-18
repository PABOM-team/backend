package com.pabom.backend.species.presentation.docs;

public final class SpeciesOpenApiExamples {

    private SpeciesOpenApiExamples() {
    }

    public static final String SPECIES_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":[
              {"id":1,"code":"CRESTED","commonName":"크레스티드 게코","category":"LIZARD","displayOrder":1},
              {"id":2,"code":"LEOPARD","commonName":"레오파드 게코","category":"LIZARD","displayOrder":2},
              {"id":3,"code":"BALL_PYTHON","commonName":"볼파이톤","category":"SNAKE","displayOrder":1}
            ]},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/species"}}
            """;

    public static final String CRESTED_MORPHS_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":[
              {"id":1,"code":"NORMAL","name":"노멀","displayOrder":1},
              {"id":2,"code":"HARLEQUIN","name":"할리퀸","displayOrder":2},
              {"id":3,"code":"FLAME","name":"플레임","displayOrder":3},
              {"id":4,"code":"DALMATIAN","name":"달마시안","displayOrder":4},
              {"id":5,"code":"PINSTRIPE","name":"핀스트라이프","displayOrder":5},
              {"id":6,"code":"LILLY_WHITE","name":"릴리 화이트","displayOrder":6}
            ]},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/species/1/morphs"}}
            """;

    public static final String LEOPARD_MORPHS_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":[
              {"id":7,"code":"NORMAL","name":"노멀","displayOrder":1},
              {"id":8,"code":"HIGH_YELLOW","name":"하이옐로","displayOrder":2},
              {"id":9,"code":"TANGERINE","name":"탠저린","displayOrder":3},
              {"id":10,"code":"MACK_SNOW","name":"맥스노우","displayOrder":4},
              {"id":11,"code":"BLIZZARD","name":"블리자드","displayOrder":5},
              {"id":12,"code":"RAPTOR","name":"랩터","displayOrder":6}
            ]},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/species/2/morphs"}}
            """;

    public static final String BALL_PYTHON_MORPHS_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":[
              {"id":13,"code":"NORMAL","name":"노멀","displayOrder":1},
              {"id":14,"code":"BANANA","name":"바나나","displayOrder":2},
              {"id":15,"code":"MOJAVE","name":"모하베","displayOrder":3},
              {"id":16,"code":"PIED","name":"파이드","displayOrder":4},
              {"id":17,"code":"CLOWN","name":"클라운","displayOrder":5}
            ]},"error":null,"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/species/3/morphs"}}
            """;

    public static final String SPECIES_NOT_FOUND = """
            {"resultType":"FAIL","success":null,"error":{"code":"SPECIES_NOT_FOUND","message":"종을 찾을 수 없습니다.","details":null},"meta":{"timestamp":"2026-10-18T07:52:18.222","path":"/api/v1/species/999/morphs"}}
            """;
}
