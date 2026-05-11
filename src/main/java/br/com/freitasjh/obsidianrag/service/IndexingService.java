package br.com.freitasjh.obsidianrag.service;

import br.com.freitasjh.obsidianrag.config.ObsidianConfig;
import br.com.freitasjh.obsidianrag.config.RagConfig;
import br.com.freitasjh.obsidianrag.model.Chunk;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.obsidian.VaultLoader;
import br.com.freitasjh.obsidianrag.obsidian.chunking.ChunkingPipeline;
import br.com.freitasjh.obsidianrag.rag.embeddings.OllamaEmbeddingProvider;
import br.com.freitasjh.obsidianrag.rag.retriever.InMemoryRetriever;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    InMemoryRetriever retriever;

    @Inject
    OllamaEmbeddingProvider embeddingProvider;

    @Inject
    RagConfig ragConfig;

    @Inject
    ObsidianConfig obsidianConfig;

    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private final AtomicInteger indexedChunks = new AtomicInteger(0);
    private volatile long lastIndexTime = 0;

    public void indexDocument(VaultDocument doc) {
        List<Chunk> chunks = chunkingPipeline.process(doc);
        for (Chunk chunk : chunks) {
            Map<String, Object> metadata = Map.of(
                    "source", chunk.getMetadata().getSource(),
                    "heading", chunk.getMetadata().getHeading() != null ? chunk.getMetadata().getHeading() : "",
                    "parentHeading", chunk.getMetadata().getParentHeading() != null ? chunk.getMetadata().getParentHeading() : "",
                    "tags", chunk.getMetadata().getTags() != null ? chunk.getMetadata().getTags() : List.of(),
                    "vault", chunk.getMetadata().getVault(),
                    "chunkIndex", chunk.getMetadata().getChunkIndex(),
                    "totalChunks", chunk.getMetadata().getTotalChunks()
            );
            retriever.index(chunk.getId(), chunk.getContent(), metadata);
        }
        indexedChunks.addAndGet(chunks.size());
        lastIndexTime = System.currentTimeMillis();
        LOG.infof("Indexed document: %s (%d chunks)", doc.getPath(), chunks.size());
    }

    public void removeDocument(String path) {
        retriever.removeByPrefix(path + "_chunk_");
        LOG.infof("Removed chunks for: %s", path);
    }

    public String reindex(boolean full) {
        if (!indexing.compareAndSet(false, true)) {
            return "Indexing already in progress";
        }

        String result;
        try {
            LOG.info("Starting " + (full ? "full" : "incremental") + " reindex...");

            if (full) {
                retriever.clear();
                embeddingProvider.clearCache();
            }

            List<VaultDocument> documents = vaultLoader.loadVault();
            int totalChunks = 0;

            for (VaultDocument doc : documents) {
                List<Chunk> chunks = chunkingPipeline.process(doc);

                for (Chunk chunk : chunks) {
                    Map<String, Object> metadata = Map.of(
                            "source", chunk.getMetadata().getSource(),
                            "heading", chunk.getMetadata().getHeading() != null ? chunk.getMetadata().getHeading() : "",
                            "parentHeading", chunk.getMetadata().getParentHeading() != null ? chunk.getMetadata().getParentHeading() : "",
                            "tags", chunk.getMetadata().getTags() != null ? chunk.getMetadata().getTags() : List.of(),
                            "vault", chunk.getMetadata().getVault(),
                            "chunkIndex", chunk.getMetadata().getChunkIndex(),
                            "totalChunks", chunk.getMetadata().getTotalChunks()
                    );

                    retriever.index(chunk.getId(), chunk.getContent(), metadata);
                    totalChunks++;
                }
            }

            indexedChunks.set(totalChunks);
            lastIndexTime = System.currentTimeMillis();

            result = "Reindex completed: %d documents, %d chunks indexed".formatted(documents.size(), totalChunks);
            LOG.info(result);

        } catch (Exception e) {
            LOG.errorf(e, "Reindex failed");
            result = "Reindex failed: " + e.getMessage();
        } finally {
            indexing.set(false);
        }

        return result;
    }

    public boolean isIndexing() {
        return indexing.get();
    }

    public int getIndexedChunkCount() {
        return indexedChunks.get();
    }

    public long getLastIndexTime() {
        return lastIndexTime;
    }

    public Map<String, Object> getIndexingStatus() {
        return Map.of(
                "isIndexing", indexing.get(),
                "indexedChunks", indexedChunks.get(),
                "totalDocuments", retriever.count(),
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
