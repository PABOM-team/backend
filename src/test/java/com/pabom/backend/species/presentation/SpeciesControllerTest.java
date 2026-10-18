package com.pabom.backend.species.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.species.application.result.MorphResult;
import com.pabom.backend.species.application.result.SpeciesResult;
import com.pabom.backend.species.application.service.SpeciesQueryService;
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
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(SpeciesQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SpeciesController(service))
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
    void returnsMorphsWithoutAuthentication() throws Exception {
        given(service.getMorphs(1L)).willReturn(List.of(
                new MorphResult(1L, "NORMAL", "노멀", 1)
        ));

        mockMvc.perform(get("/api/v1/species/1/morphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data[0].code").value("NORMAL"));
    }
}
