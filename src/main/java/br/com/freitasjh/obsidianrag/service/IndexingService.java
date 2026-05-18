package br.com.freitasjh.obsidianrag.service;

import br.com.freitasjh.obsidianrag.config.ObsidianConfig;
import br.com.freitasjh.obsidianrag.config.RagConfig;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.obsidian.VaultLoader;
import br.com.freitasjh.obsidianrag.obsidian.chunking.ChunkingPipeline;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class IndexingService {

    private static final Logger LOG = Logger.getLogger(IndexingService.class);

    @Inject
    VaultLoader vaultLoader;

    @Inject
    ChunkingPipeline chunkingPipeline;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    InMemoryEmbeddingStore<TextSegment> embeddingStore;

    @Inject
    RagConfig ragConfig;

    @Inject
    ObsidianConfig obsidianConfig;

    private final Set<String> knownIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private final AtomicInteger indexedChunks = new AtomicInteger(0);
    private volatile long lastIndexTime = 0;

    @PostConstruct
    void init() {
        for (VaultDocument doc : vaultLoader.loadVault()) {
            List<TextSegment> segments = chunkingPipeline.process(doc);
            for (int i = 0; i < segments.size(); i++) {
                knownIds.add(doc.getPath() + "_chunk_" + i);
            }
        }
        indexedChunks.set(knownIds.size());
        LOG.infof("Initialized with %d known segment IDs from vault", knownIds.size());
    }

    public void indexDocument(VaultDocument doc) {
        List<TextSegment> segments = chunkingPipeline.process(doc);
        List<Embedding> embeddings = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            embeddings.add(embeddingModel.embed(segments.get(i)).content());
            String id = doc.getPath() + "_chunk_" + i;
            ids.add(id);
            knownIds.add(id);
        }

        embeddingStore.addAll(ids, embeddings, segments);
        indexedChunks.addAndGet(segments.size());
        lastIndexTime = System.currentTimeMillis();
        LOG.infof("Indexed document: %s (%d segments)", doc.getPath(), segments.size());
    }

    public void removeDocument(String path) {
        List<String> idsToRemove = knownIds.stream()
                .filter(id -> id.startsWith(path))
                .toList();
        if (!idsToRemove.isEmpty()) {
            embeddingStore.removeAll(idsToRemove);
            knownIds.removeAll(idsToRemove);
        }
        LOG.infof("Removed %d segments for: %s", idsToRemove.size(), path);
    }

    public String reindex(boolean full) {
        if (!indexing.compareAndSet(false, true)) {
            return "Indexing already in progress";
        }

        String result;
        try {
            LOG.info("Starting " + (full ? "full" : "incremental") + " reindex...");

            if (full) {
                List<String> allIds = new ArrayList<>(knownIds);
                if (!allIds.isEmpty()) {
                    embeddingStore.removeAll(allIds);
                }
                knownIds.clear();
            }

            List<VaultDocument> documents = vaultLoader.loadVault();
            int totalSegments = 0;

            for (VaultDocument doc : documents) {
                List<TextSegment> segments = chunkingPipeline.process(doc);
                List<Embedding> embeddings = new ArrayList<>();
                List<String> ids = new ArrayList<>();

                for (int i = 0; i < segments.size(); i++) {
                    embeddings.add(embeddingModel.embed(segments.get(i)).content());
                    String id = doc.getPath() + "_chunk_" + i;
                    ids.add(id);
                    knownIds.add(id);
                }

                embeddingStore.addAll(ids, embeddings, segments);
                totalSegments += segments.size();
            }

            indexedChunks.set(totalSegments);
            lastIndexTime = System.currentTimeMillis();

            result = "Reindex completed: %d documents, %d segments indexed".formatted(documents.size(), totalSegments);
            LOG.info(result);

        } catch (Exception e) {
            LOG.errorf(e, "Reindex failed");
            result = "Reindex failed: " + e.getMessage();
        } finally {
            indexing.set(false);
        }

        return result;
    }

    public boolean isIndexing() { return indexing.get(); }
    public int getIndexedChunkCount() { return indexedChunks.get(); }
    public long getLastIndexTime() { return lastIndexTime; }

    public Map<String, Object> getIndexingStatus() {
        return Map.of(
                "isIndexing", indexing.get(),
                "indexedChunks", indexedChunks.get(),
                "lastIndexTime", lastIndexTime
        );
    }

    public List<String> indexInBackground() {
        List<String> messages = new ArrayList<>();

        if (indexing.get()) {
            messages.add("Indexing already in progress");
            return messages;
        }

        messages.add("Starting background indexing...");
        reindex(true);
        messages.add("Background indexing completed");

        return messages;
    }
}
