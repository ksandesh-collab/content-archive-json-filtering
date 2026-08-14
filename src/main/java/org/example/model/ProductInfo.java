package org.example.model;

public record ProductInfo(
        IdNameStatus product,
        IdNameStatus productGroup,
        IdNameStatus productSubGroup
) {
}
