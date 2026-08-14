package org.example.model;

public record PubSeriesInfo(
        boolean analystBookshelf,
        AssetClassInfo assetClassInfo,
        boolean debtRestricted,
        String displayName,
        CodeName frequency,
        int id,
        CodeName language,
        String name,
        ProductInfo productInfo,
        PubSeriesGroup pubSeriesGroup,
        String status
) {
}
