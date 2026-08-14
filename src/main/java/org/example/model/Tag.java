package org.example.model;

public record Tag(
        String code,
        TagDetail detail,
        boolean internal,
        String type
) {
}
