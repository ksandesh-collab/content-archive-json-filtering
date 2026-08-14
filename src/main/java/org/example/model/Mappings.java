package org.example.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record Mappings(
        List<JsonNode> affiliates,
        List<JsonNode> assetClassGroups,
        List<JsonNode> assetClasses,
        String conflictOfInterest,
        List<JsonNode> countries,
        String coverageNameOverride,
        List<JsonNode> coverageRegions,
        List<JsonNode> coverages,
        List<JsonNode> entitlementAccessGroups,
        List<JsonNode> extRegions,
        List<JsonNode> externalAttr,
        List<JsonNode> languages,
        String lastPubDate,
        List<JsonNode> products,
        List<JsonNode> refProducts,
        List<JsonNode> refSubProducts,
        String sec_15a6_exemption,
        List<JsonNode> sectors,
        List<JsonNode> subAssetClasses,
        List<JsonNode> subProducts
) {
}
