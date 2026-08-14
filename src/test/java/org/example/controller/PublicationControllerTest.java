package org.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicationControllerTest {

    private static final String PUB_ID = "6a7f7d42ce691f11f98501e6";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsOnlyRequestedFields() throws Exception {
        mockMvc.perform(get("/api/publications")
                        .param("pubId", PUB_ID)
                        .param("includeOutputField", "basicInfo.pubDate,basicInfo.pubId,titles.type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].basicInfo.pubId").value(PUB_ID))
                .andExpect(jsonPath("$[0].basicInfo.pubDate").exists())
                .andExpect(jsonPath("$[0].basicInfo.source").doesNotExist())
                .andExpect(jsonPath("$[0].titles[0].type").exists())
                .andExpect(jsonPath("$[0].titles[0].value").doesNotExist());
    }

    @Test
    void returnsFullDocumentWhenNoFieldsRequested() throws Exception {
        mockMvc.perform(get("/api/publications")
                        .param("pubId", PUB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].basicInfo.source").value("ANALYTICS"))
                .andExpect(jsonPath("$[0].pubUrlInfo").exists());
    }

    @Test
    void supportsMultiplePubIdsPreservingOrderAndSkippingMisses() throws Exception {
        mockMvc.perform(get("/api/publications")
                        .param("pubId", PUB_ID, "b1c2d3e4f5a6b7c8d9e0f1a2", "does-not-exist")
                        .param("includeOutputField", "basicInfo.pubId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].basicInfo.pubId").value(PUB_ID))
                .andExpect(jsonPath("$[1].basicInfo.pubId").value("b1c2d3e4f5a6b7c8d9e0f1a2"));
    }

    @Test
    void returnsNotFoundWhenNoPubIdMatches() throws Exception {
        mockMvc.perform(get("/api/publications").param("pubId", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestWhenPubIdMissing() throws Exception {
        mockMvc.perform(get("/api/publications"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersArrayElementsByValuePredicate() throws Exception {
        mockMvc.perform(get("/api/publications")
                        .param("pubId", PUB_ID)
                        .param("includeOutputField", "$.titles[?(@.type == 'L1')].value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titles.length()").value(1))
                .andExpect(jsonPath("$[0].titles[0].value").value("Analytics"))
                .andExpect(jsonPath("$[0].titles[0].type").doesNotExist());
    }

    @Test
    void filtersNestedTagDetailMappingsCoverages() throws Exception {
        mockMvc.perform(get("/api/publications")
                        .param("pubId", PUB_ID)
                        .param("includeOutputField", "tags[?(@.type == 'ANALYST')].detail.mappings.coverages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tags.length()").value(1))
                .andExpect(jsonPath("$[0].tags[0].detail.mappings.coverages").isArray())
                .andExpect(jsonPath("$[0].tags[0].detail.mappings.coverages.length()").value(0));
    }
}
