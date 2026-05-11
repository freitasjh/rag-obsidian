package br.com.freitasjh.obsidianrag.rag.retriever;

import br.com.freitasjh.obsidianrag.model.SearchResult;
import br.com.freitasjh.obsidianrag.rag.embeddings.EmbeddingProvider;
import br.com.freitasjh.obsidianrag.rag.embeddings.OllamaEmbeddingProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class InMemoryRetriever implements SemanticRetriever {

    private static final Logger LOG = Logger.getLogger(InMemoryRetriever.class);

    private final Map<String, IndexedDocument> documents = new ConcurrentHashMap<>();

    @Inject
    OllamaEmbeddingProvider embeddingProvider;

    @Override
    public List<SearchResult> search(String query, int limit) {
        if (documents.isEmpty()) {
            LOG.warn("No documents indexed for search");
            return Collections.emptyList();
        }

        EmbeddingProvider.Embedding queryEmbedding = embeddingProvider.embed(query);
        List<Double> queryVector = queryEmbedding.getValues();

        String lowerQuery = query.toLowerCase();
        String[] queryTerms = lowerQuery.split("\\s+");

        List<ScoredDocument> scored = documents.values().stream()
                .map(doc -> {
                    double semanticScore = cosineSimilarity(queryVector, doc.embedding);
                    double keywordBoost = keywordScore(doc.content, lowerQuery, queryTerms);
                    double combined = semanticScore + keywordBoost;
                    return new ScoredDocument(doc, combined, semanticScore, keywordBoost);
                })
                .filter(sd -> sd.semanticScore > 0.35 || sd.keywordBoost > 0)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .collect(Collectors.toList());

        LOG.infof("Search returned %d results (out of %d indexed)", scored.size(), documents.size());

        return scored.stream()
                .map(sd -> new SearchResult(
                        sd.document.content,
                        sd.score,
                        sd.document.metadata
                ))
                .collect(Collectors.toList());
    }

    private double keywordScore(String content, String lowerQuery, String[] queryTerms) {
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains(lowerQuery)) {
            return 0.3;
        }
        long matchCount = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 && lowerContent.contains(term)) {
                matchCount++;
            }
        }
        if (queryTerms.length == 0) return 0;
        return (double) matchCount / queryTerms.length * 0.2;
    }

    @Override
    public void index(String id, String content, Map<String, Object> metadata) {
        EmbeddingProvider.Embedding embedding = embeddingProvider.embed(content);

        IndexedDocument doc = new IndexedDocument(id, content, embedding.getValues(), metadata);
        documents.put(id, doc);

        LOG.debugf("Indexed document: %s", id);
    }

    @Override
    public void remove(String id) {
        documents.remove(id);
        LOG.debugf("Removed document: %s", id);
    }

    public void removeByPrefix(String prefix) {
        documents.keySet().removeIf(id -> id.startsWith(prefix));
        LOG.debugf("Removed documents with prefix: %s", prefix);
    }

    @Override
    public void clear() {
        documents.clear();
        LOG.info("Cleared all indexed documents");
    }

    @Override
    public int count() {
        return documents.size();
    }

    private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            magnitude1 += vec1.get(i) * vec1.get(i);
            magnitude2 += vec2.get(i) * vec2.get(i);
        }

        double magProduct = Math.sqrt(magnitude1) * Math.sqrt(magnitude2);

        if (magProduct == 0) {
            return 0.0;
        }

        return dotProduct / magProduct;
    }

    public List<SearchResult> getAllDocuments() {
        return documents.values().stream()
                .map(doc -> new SearchResult(
                        doc.content,
                        1.0,
                        doc.metadata
                ))
                .collect(Collectors.toList());
    }

    public Optional<SearchResult> getDocument(String id) {
        IndexedDocument doc = documents.get(id);
        if (doc != null) {
            return Optional.of(new SearchResult(doc.content, 1.0, doc.metadata));
        }
        return Optional.empty();
    }

    private static class IndexedDocument {
        final String id;
        final String content;
        final List<Double> embedding;
        final Map<String, Object> metadata;

        IndexedDocument(String id, String content, List<Double> embedding, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.embedding = embedding;
            this.metadata = metadata;
        }
    }

    private static class ScoredDocument {
        final IndexedDocument document;
        final double score;
        final double semanticScore;
        final double keywordBoost;

        ScoredDocument(IndexedDocument document, double score, double semanticScore, double keywordBoost) {
            this.document = document;
            this.score = score;
            this.semanticScore = semanticScore;
            this.keywordBoost = keywordBoost;
        }
    }
}
