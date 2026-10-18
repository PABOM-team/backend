package com.pabom.backend.species.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.species.application.result.MorphResult;
import com.pabom.backend.species.application.result.SpeciesResult;
import com.pabom.backend.species.domain.type.ReptileCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-species-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class SpeciesQueryServiceIntegrationTest {

    @Autowired
    private SpeciesQueryService service;

    @Test
    void returnsSpeciesByCategoryAndDisplayOrder() {
        List<SpeciesResult> species = service.getSpecies();

        assertThat(species).extracting(SpeciesResult::code)
                .containsExactly("CRESTED", "LEOPARD", "BALL_PYTHON");
        assertThat(species).extracting(SpeciesResult::category)
                .containsExactly(ReptileCategory.LIZARD, ReptileCategory.LIZARD, ReptileCategory.SNAKE);
        assertThat(species).extracting(SpeciesResult::displayOrder).containsExactly(1, 2, 1);
    }

    @Test
    void returnsIsolatedMorphsInDisplayOrder() {
        SpeciesResult crested = species("CRESTED");
        SpeciesResult leopard = species("LEOPARD");
        SpeciesResult ballPython = species("BALL_PYTHON");

        assertThat(service.getMorphs(crested.id())).extracting(MorphResult::code)
                .containsExactly("NORMAL", "HARLEQUIN", "FLAME", "DALMATIAN", "PINSTRIPE", "LILLY_WHITE");
        assertThat(service.getMorphs(leopard.id())).extracting(MorphResult::code)
                .containsExactly("NORMAL", "HIGH_YELLOW", "TANGERINE", "MACK_SNOW", "BLIZZARD", "RAPTOR");
        assertThat(service.getMorphs(ballPython.id())).extracting(MorphResult::code)
                .containsExactly("NORMAL", "BANANA", "MOJAVE", "PIED", "CLOWN");
    }

    private SpeciesResult species(String code) {
        return service.getSpecies().stream()
                .filter(item -> item.code().equals(code))
                .findFirst()
                .orElseThrow();
    }
}
