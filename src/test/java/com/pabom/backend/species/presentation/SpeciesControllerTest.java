package com.pabom.backend.species.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.species.application.result.MorphResult;
import com.pabom.backend.species.application.result.SpeciesResult;
import com.pabom.backend.species.application.service.SpeciesQueryService;
import com.pabom.backend.careguideline.application.service.SpeciesCareStandardsQueryService;
import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult;
import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult.CareItemStandard;
import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult.LifeStageStandards;
import com.pabom.backend.species.domain.type.ReptileCategory;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpeciesControllerTest {

    private SpeciesQueryService service;
    private SpeciesCareStandardsQueryService careStandardsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(SpeciesQueryService.class);
        careStandardsService = org.mockito.Mockito.mock(SpeciesCareStandardsQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SpeciesController(service, careStandardsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSpeciesWithoutAuthentication() throws Exception {
        given(service.getSpecies()).willReturn(List.of(
                new SpeciesResult(1L, "CRESTED", "크레스티드 게코", ReptileCategory.LIZARD, 1)
        ));

        mockMvc.perform(get("/api/v1/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data[0].code").value("CRESTED"));
    }

    @Test
    void returnsCareStandardsWithUnknownFallbackWithoutAuthentication() throws Exception {
        given(careStandardsService.get(1L)).willReturn(new SpeciesCareStandardsResult(
                "1.2", 1L, "LEOPARD", "레오파드 게코", List.of(
                new LifeStageStandards("UNKNOWN", "JUVENILE", List.of(
                        new CareItemStandard("WEIGHT", "체중", 7, 7, 14, true,
                                "WEIGHT", null, null, "TEXTBOOK", "Mader", "공통 설명"))))));

        mockMvc.perform(get("/api/v1/species/1/care-standards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.guidelineVersion").value("1.2"))
                .andExpect(jsonPath("$.success.data.lifeStages[0].appliedLifeStage").value("JUVENILE"));
    }

    @Test
    void returnsMorphsWithoutAuthentication() throws Exception {
        given(service.getMorphs(1L)).willReturn(List.of(
                new MorphResult(1L, "NORMAL", "노멀", 1)
        ));

        mockMvc.perform(get("/api/v1/species/1/morphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data[0].code").value("NORMAL"));
    }
}
