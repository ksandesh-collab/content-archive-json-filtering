package org.example.service;

import org.example.model.Publication;
import org.example.store.PublicationStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicationSearchService {

    private final PublicationStore publicationStore;

    public PublicationSearchService(PublicationStore publicationStore) {
        this.publicationStore = publicationStore;
    }

    /**
     * Looks up each pubId. Results are returned in the same order as pubIds;
     * pubIds with no match are simply omitted. Field projection is applied
     * later, at the HTTP response boundary, not here.
     */
    public List<Publication> search(List<String> pubIds) {
        List<Publication> results = new ArrayList<>();
        for (String pubId : pubIds) {
            Publication publication = publicationStore.findByPubId(pubId);
            if (publication != null) {
                results.add(publication);
            }
        }
        return results;
    }
}
