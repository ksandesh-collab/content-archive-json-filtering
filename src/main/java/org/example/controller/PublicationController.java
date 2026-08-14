package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.model.Publication;
import org.example.service.PublicationSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {

    /** Request attribute {@link org.example.web.IncludeFieldsResponseAdvice} reads to know which fields to keep. */
    public static final String INCLUDE_OUTPUT_FIELD_ATTRIBUTE = "includeOutputField";

    private final PublicationSearchService publicationSearchService;

    public PublicationController(PublicationSearchService publicationSearchService) {
        this.publicationSearchService = publicationSearchService;
    }

    /**
     * GET /api/publications?pubId=a&pubId=b&includeOutputField=basicInfo.pubDate,basicInfo.pubId,tags.tagType
     * <p>
     * pubId may be repeated or comma-separated. includeOutputField is optional;
     * when omitted, the full publication document is returned for each match.
     * This method returns the typed {@link Publication} model as-is; the
     * actual field projection happens right before serialization in
     * {@link org.example.web.IncludeFieldsResponseAdvice}.
     */
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam("pubId") List<String> pubIds,
            @RequestParam(value = "includeOutputField", required = false) List<String> includeOutputField,
            HttpServletRequest request) {

        List<Publication> results = publicationSearchService.search(pubIds);
        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No publications found for the given pubId(s): " + pubIds));
        }
        if (includeOutputField != null && !includeOutputField.isEmpty()) {
            request.setAttribute(INCLUDE_OUTPUT_FIELD_ATTRIBUTE, includeOutputField);
        }
        return ResponseEntity.ok(results);
    }

    record ErrorResponse(String message) {
    }
}
