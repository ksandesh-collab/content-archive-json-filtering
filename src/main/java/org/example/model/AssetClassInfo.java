package org.example.model;

public record AssetClassInfo(
        IdNameStatus assetClass,
        IdNameStatus assetClassGroup,
        IdNameStatus assetClassSubGroup,
        IdNameStatus subAssetClass
) {
}
