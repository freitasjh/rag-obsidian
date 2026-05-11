package br.com.freitasjh.obsidianrag.service;

import br.com.freitasjh.obsidianrag.model.SearchResult;
import br.com.freitasjh.obsidianrag.rag.retriever.InMemoryRetriever;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class SearchService {

    private static final Logger LOG = Logger.getLogger(SearchService.class);

    @Inject
    InMemoryRetriever retriever;

    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            LOG.warn("Empty query received");
            return List.of();
        }

        LOG.infof("Searching for: '%s' with limit: %d", query, limit);
        List<SearchResult> results = retriever.search(query, limit);
        LOG.infof("Found %d results", results.size());

        return results;
    }

    public List<SearchResult> searchWithDefaults(String query) {
        return search(query, 5);
    }
}
