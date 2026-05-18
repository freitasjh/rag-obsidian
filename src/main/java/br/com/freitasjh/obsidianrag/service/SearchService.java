package br.com.freitasjh.obsidianrag.service;

import br.com.freitasjh.obsidianrag.model.SearchResult;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SearchService {

    private static final Logger LOG = Logger.getLogger(SearchService.class);

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            LOG.warn("Empty query received");
            return List.of();
        }

        LOG.infof("Searching for: '%s' with limit: %d", query, limit);

        var request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(query).content())
                .maxResults(limit)
                .build();

        List<SearchResult> results = embeddingStore.search(request).matches().stream()
                .map(match -> new SearchResult(
                        match.embedded().text(),
                        match.score(),
                        match.embedded().metadata().toMap()
                ))
                .collect(Collectors.toList());

        LOG.infof("Found %d results", results.size());
        return results;
    }

    public List<SearchResult> searchWithDefaults(String query) {
        return search(query, 5);
    }
}
