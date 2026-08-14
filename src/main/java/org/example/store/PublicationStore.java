package org.example.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.model.Publication;
import org.example.model.Title;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory publication data store, seeded at startup from
 * src/main/resources/Publication.json, which follows the content-archive
 * search response envelope: {@code data.publications[]}. Two extra clones
 * (with different pubId/title) of the first entry are added purely as
 * sample data so multi-pubId search has more than one record to return.
 */
@Component
public class PublicationStore {

    private final ObjectMapper objectMapper;
    private final Map<String, Publication> publicationsByPubId = new LinkedHashMap<>();

    public PublicationStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() throws IOException {
        JsonNode root;
        try (InputStream in = new ClassPathResource("Publication.json").getInputStream()) {
            root = objectMapper.readTree(in);
        }
        JsonNode publicationsNode = root.path("data").path("publications");
        if (!publicationsNode.isArray() || publicationsNode.isEmpty()) {
            throw new IllegalStateException("Publication.json must contain at least one entry under data.publications");
        }

        List<Publication> basePublications = new ArrayList<>();
        for (JsonNode publicationNode : publicationsNode) {
            basePublications.add(objectMapper.treeToValue(publicationNode, Publication.class));
        }
        basePublications.forEach(this::index);

        Publication seed = basePublications.get(0);
        index(withIdentity(seed, "b1c2d3e4f5a6b7c8d9e0f1a2", "CMBS Smorgasbord: Spread Update"));
        index(withIdentity(seed, "c2d3e4f5a6b7c8d9e0f1a2b3", "CMBS Smorgasbord: Issuance Recap"));
    }

    public Publication findByPubId(String pubId) {
        return publicationsByPubId.get(pubId);
    }

    private void index(Publication publication) {
        publicationsByPubId.put(publication.basicInfo().pubId(), publication);
    }

    private Publication withIdentity(Publication base, String newPubId, String newTitleValue) {
        Publication withId = base.withBasicInfo(base.basicInfo().withPubId(newPubId));
        List<Title> titles = new ArrayList<>(withId.titles());
        if (!titles.isEmpty()) {
            titles.set(0, titles.get(0).withValue(newTitleValue));
        }
        return withId.withTitles(titles);
    }
}
