package org.example.model;

public record Title(String language, String type, String value) {

    public Title withValue(String value) {
        return new Title(language, type, value);
    }
}
