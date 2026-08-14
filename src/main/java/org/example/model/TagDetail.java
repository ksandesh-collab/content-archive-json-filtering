package org.example.model;

public record TagDetail(
        String email,
        int id,
        String imageUrl,
        String lbCode,
        Mappings mappings,
        String name,
        int regionId,
        String status,
        String thumbnailUrl,
        String userType
) {
}
