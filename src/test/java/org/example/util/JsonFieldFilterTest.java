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

    @Test
    void supportsPredicateAndProjectionNestedArbitrarilyDeep() throws Exception {
        JsonNode deepSource = objectMapper.readTree("""
                {
                  "data": {
                    "publications": [
                      {
                        "tags": [
                          {
                            "type": "ANALYST",
                            "detail": {
                              "mappings": {
                                "refSubProducts": [
                                  { "id": 1, "product": { "id": 100, "name": "A" } },
                                  { "id": 2, "product": { "id": 200, "name": "B" } }
                                ]
                              }
                            }
                          },
                          {
                            "type": "OTHER",
                            "detail": {
                              "mappings": {
                                "refSubProducts": [
                                  { "id": 9, "product": { "id": 900, "name": "Z" } }
                                ]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
                """);

        JsonNode result = JsonFieldFilter.filter(deepSource, List.of(
                "data.publications.tags[?(@.type == 'ANALYST')].detail.mappings.refSubProducts.id",
                "data.publications.tags[?(@.type == 'ANALYST')].detail.mappings.refSubProducts.product.id"
        ));

        JsonNode tags = result.get("data").get("publications").get(0).get("tags");
        assertThat(tags).hasSize(1);
        JsonNode refSubProducts = tags.get(0).get("detail").get("mappings").get("refSubProducts");
        assertThat(refSubProducts).hasSize(2);
        assertThat(refSubProducts.get(0).get("id").asInt()).isEqualTo(1);
        assertThat(refSubProducts.get(0).get("product").get("id").asInt()).isEqualTo(100);
        assertThat(refSubProducts.get(0).get("product").has("name")).isFalse();
        assertThat(refSubProducts.get(1).get("id").asInt()).isEqualTo(2);
        assertThat(refSubProducts.get(1).get("product").get("id").asInt()).isEqualTo(200);
    }

    @Test
    void supportsPredicatesAtMultipleNestingLevelsInOnePath() throws Exception {
        JsonNode deepSource = objectMapper.readTree("""
                {
                  "data": {
                    "publications": [
                      { "pubId": "p1", "tags": [ { "type": "ANALYST", "detail": { "mappings": { "refSubProducts": [
                          { "id": 1, "product": { "id": 100 } }, { "id": 2, "product": { "id": 200 } } ] } } } ] },
                      { "pubId": "p2", "tags": [ { "type": "ANALYST", "detail": { "mappings": { "refSubProducts": [
                          { "id": 9, "product": { "id": 900 } } ] } } } ] }
                    ]
                  }
                }
                """);

        JsonNode result = JsonFieldFilter.filter(deepSource, List.of(
                "data.publications[?(@.pubId == 'p2')].tags[?(@.type == 'ANALYST')].detail.mappings.refSubProducts[?(@.id == 9)].product.id"
        ));

        JsonNode publications = result.get("data").get("publications");
        assertThat(publications).hasSize(1);
        JsonNode refSubProducts = publications.get(0).get("tags").get(0).get("detail").get("mappings").get("refSubProducts");
        assertThat(refSubProducts).hasSize(1);
        assertThat(refSubProducts.get(0).get("product").get("id").asInt()).isEqualTo(900);
    }

    @Test
    void combinesDistinctPredicatesOnTheSameArrayFieldAcrossPathsWithOrSemantics() throws Exception {
        JsonNode source = objectMapper.readTree("""
                {
                  "tags": [
                    { "type": "COVER_ANALYST", "value": "A" },
                    { "type": "ANALYST", "value": "B" },
                    { "type": "SECONDARY_CONTRIBUTOR", "value": "C" },
                    { "type": "OTHER_TYPE", "value": "D" }
                  ]
                }
                """);

        JsonNode result = JsonFieldFilter.filter(source, List.of(
                "tags[?(@.type == 'COVER_ANALYST')].value",
                "tags[?(@.type == 'ANALYST')].value",
                "tags[?(@.type == 'SECONDARY_CONTRIBUTOR')].value"
        ));

        assertThat(result.get("tags")).hasSize(3);
        List<String> values = new java.util.ArrayList<>();
        result.get("tags").forEach(tag -> values.add(tag.get("value").asText()));
        assertThat(values).containsExactly("A", "B", "C");
    }

    @Test
    void unconditionalPathOverridesPredicatesOnSameField() throws Exception {
        JsonNode result = JsonFieldFilter.filter(sample(), List.of(
                "tags[?(@.tagType == 'ANALYST')].value",
                "tags.tagType"
        ));

        assertThat(result.get("tags")).hasSize(3);
    }
}
