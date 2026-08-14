package org.example.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFieldFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode sample() throws Exception {
        return objectMapper.readTree("""
                {
                  "basicInfo": { "pubId": "abc", "pubDate": "2026-08-14" },
                  "tags": [
                    { "tagType": "ANALYST", "value": "Jane Doe" },
                    { "tagType": "DESK", "value": "Credit Desk" },
                    { "tagType": "ANALYST", "value": "John Smith" }
                  ]
                }
                """);
    }

    @Test
    void keepsOnlyArrayElementsMatchingPredicate() throws Exception {
        JsonNode result = JsonFieldFilter.filter(sample(), List.of("tags[?(@.tagType == 'ANALYST')]"));

        assertThat(result.get("tags")).hasSize(2);
        assertThat(result.get("tags").get(0).get("value").asText()).isEqualTo("Jane Doe");
        assertThat(result.get("tags").get(1).get("value").asText()).isEqualTo("John Smith");
        assertThat(result.has("basicInfo")).isFalse();
    }

    @Test
    void combinesPredicateWithFieldProjection() throws Exception {
        JsonNode result = JsonFieldFilter.filter(sample(), List.of("tags[?(@.tagType == 'ANALYST')].value"));

        assertThat(result.get("tags")).hasSize(2);
        assertThat(result.get("tags").get(0).has("tagType")).isFalse();
        assertThat(result.get("tags").get(0).get("value").asText()).isEqualTo("Jane Doe");
    }

    @Test
    void supportsRootPrefixAndNotEqualsOperator() throws Exception {
        JsonNode result = JsonFieldFilter.filter(sample(), List.of("$.tags[?(@.tagType != 'ANALYST')]"));

        assertThat(result.get("tags")).hasSize(1);
        assertThat(result.get("tags").get(0).get("tagType").asText()).isEqualTo("DESK");
    }

    @Test
    void returnsEmptyArrayWhenNothingMatches() throws Exception {
        JsonNode result = JsonFieldFilter.filter(sample(), List.of("tags[?(@.tagType == 'NOPE')]"));

        assertThat(result.get("tags")).isEmpty();
    }

    @Test
    void plainDotPathsStillWorkUnchanged() throws Exception {
        JsonNode result = JsonFieldFilter.filter(sample(), List.of("basicInfo.pubId"));

        assertThat(result.get("basicInfo").get("pubId").asText()).isEqualTo("abc");
        assertThat(result.get("basicInfo").has("pubDate")).isFalse();
        assertThat(result.has("tags")).isFalse();
    }
}
