package com.pabom.backend.pet.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.application.service.PetCommandService;
import com.pabom.backend.pet.application.service.PetQueryService;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.species.domain.error.SpeciesErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PetControllerTest {

    private PetCommandService commandService;
    private PetQueryService queryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = org.mockito.Mockito.mock(PetCommandService.class);
        queryService = org.mockito.Mockito.mock(PetQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PetController(commandService, queryService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(7L), null, List.of()
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPetWithAuthenticatedOwnerAndReturns201() throws Exception {
        given(commandService.create(eq(7L), any())).willReturn(result());

        mockMvc.perform(post("/api/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientRequestId":"550e8400-e29b-41d4-a716-446655440000",
                                  "speciesId":1,
                                  "name":"콩이",
                                  "morphIds":[],
                                  "hatchedAt":null,
                                  "sex":"UNKNOWN",
                                  "sexCertainty":"UNKNOWN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.success.data.id").value(10))
                .andExpect(jsonPath("$.success.data.name").value("콩이"));
    }

    @Test
    void rejectsUnauthenticatedPetRequest() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/pets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void distinguishesOmittedPatchFieldsFromExplicitNull() throws Exception {
        given(commandService.update(eq(7L), eq(10L), any())).willReturn(result());

        mockMvc.perform(patch("/api/v1/pets/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customMorph":null,"hatchedAt":null,"morphIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.id").value(10));
    }

    @Test
    void validatesRequiredCreateFields() throws Exception {
        mockMvc.perform(post("/api/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void returns422WhenConfirmedStageHasNoLifeStageId() throws Exception {
        given(commandService.create(eq(7L), any()))
                .willThrow(new BusinessException(PetErrorCode.STAGE_REQUIRED));

        mockMvc.perform(post("/api/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientRequestId":"550e8400-e29b-41d4-a716-446655440099",
                                  "speciesId":1,
                                  "name":"콩이",
                                  "stageCertainty":"CONFIRMED"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("STAGE_REQUIRED"));
    }

    @Test
    void returns422ForInvalidLifeStageOnPatch() throws Exception {
        given(commandService.update(eq(7L), eq(10L), any()))
                .willThrow(new BusinessException(SpeciesErrorCode.LIFE_STAGE_INVALID));

        mockMvc.perform(patch("/api/v1/pets/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lifeStageId":999,"stageCertainty":"CONFIRMED"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("LIFE_STAGE_INVALID"));
    }

    private PetResult result() {
        return new PetResult(
                10L, 1L, "콩이", List.of(), null, LocalDate.of(2026, 3, 2),
                null, Certainty.UNKNOWN, Sex.UNKNOWN, Certainty.UNKNOWN, null,
                Instant.parse("2026-10-18T00:00:00Z"), Instant.parse("2026-10-18T00:00:00Z")
        );
    }
}
