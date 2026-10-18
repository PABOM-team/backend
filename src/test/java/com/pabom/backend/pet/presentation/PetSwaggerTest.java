package com.pabom.backend.pet.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PetSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsSixApiSuccessResponsesWithConcreteSchemasAndExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/species'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/species/{speciesId}/morphs'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/species'].get.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/species/{speciesId}/morphs'].get.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/species'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/species/{speciesId}/morphs'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pets'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pets/{petId}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pets/{petId}'].patch.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.security[0].Authorization").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.requestBody.content['application/json'].examples['개체 등록'].value.clientRequestId")
                        .value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.paths['/api/v1/pets/{petId}'].patch.requestBody.content['application/json'].examples['개체 수정'].value.hatchedAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['201'].content['application/json'].examples['등록 성공'].value.resultType")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['201'].content['application/json'].examples['등록 성공'].value.error")
                        .doesNotExist())
                .andExpect(content().string(containsString("SpeciesListApiResponse")))
                .andExpect(content().string(containsString("MorphListApiResponse")))
                .andExpect(content().string(containsString("PetListApiResponse")))
                .andExpect(content().string(containsString("PetApiResponse")));
    }

    @Test
    void documentsAllMorphSeedExamplesAndActualErrorContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/species/{speciesId}/morphs'].get.responses['200'].content['application/json'].examples['크레스티드 게코']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/species/{speciesId}/morphs'].get.responses['200'].content['application/json'].examples['레오파드 게코']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/species/{speciesId}/morphs'].get.responses['200'].content['application/json'].examples['볼파이톤']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['400'].content['application/json'].examples.MORPH_INVALID.value.resultType")
                        .value("FAIL"))
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['400'].content['application/json'].examples.MORPH_INVALID.value.error.code")
                        .value("MORPH_INVALID"))
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['401'].content['application/json'].examples.INVALID_TOKEN.value.error.code")
                        .value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.paths['/api/v1/pets'].post.responses['404'].content['application/json'].examples.SPECIES_NOT_FOUND.value.error.code")
                        .value("SPECIES_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/pets/{petId}'].get.responses['404'].content['application/json'].examples.PET_NOT_FOUND.value.error.code")
                        .value("PET_NOT_FOUND"))
                .andExpect(content().string(containsString("MORPH_DUPLICATED")))
                .andExpect(content().string(containsString("MORPH_COMBINATION_INVALID")))
                .andExpect(content().string(containsString("CUSTOM_MORPH_INVALID")))
                .andExpect(content().string(containsString("PET_NAME_INVALID")))
                .andExpect(content().string(containsString("HATCHED_AT_INVALID")))
                .andExpect(content().string(containsString("PET_PHOTO_INVALID")))
                .andExpect(content().string(containsString("LIFE_STAGE_INVALID")));
    }

    @Test
    void exposesTypedRequestAndResponseComponentSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CreatePetRequest.properties.clientRequestId.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.CreatePetRequest.properties.clientRequestId.format").value("uuid"))
                .andExpect(jsonPath("$.components.schemas.CreatePetRequest.properties.morphIds.type", hasItem("array")))
                .andExpect(jsonPath("$.components.schemas.CreatePetRequest.properties.hatchedAt.format").value("date"))
                .andExpect(jsonPath("$.components.schemas.CreatePetRequest.required",
                        containsInAnyOrder("clientRequestId", "speciesId", "name")))
                .andExpect(jsonPath("$.components.schemas.CreatePetRequest.properties.customMorph.type", hasItem("null")))
                .andExpect(jsonPath("$.components.schemas.UpdatePetRequest.properties.customMorph.type", hasItem("null")))
                .andExpect(jsonPath("$.components.schemas.UpdatePetRequest.properties.namePresent").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.PetResponse.properties.morphs.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.PetResponse.properties.createdAt.format").value("date-time"))
                .andExpect(jsonPath("$.components.schemas.PetResponse.properties.sex.enum").isArray())
                .andExpect(jsonPath("$.components.schemas.PetResponse.properties.sexCertainty.enum").isArray())
                .andExpect(jsonPath("$.components.schemas.SpeciesResponse.properties.category.enum").isArray());
    }
}
