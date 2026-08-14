package org.example.model;

public record PubUrlInfo(
        String emailCoverUrl,
        String linkBackUrl,
        String offlineUrl,
        String pdfUrl,
        String primaryContentType,
        String primaryContentUrl,
        String textUrl,
        String thumbNailUrl,
        String viewerUrl
) {
}
