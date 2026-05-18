package br.com.freitasjh.obsidianrag.langchain4j;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class LangChain4jProducer {

    private static final Logger LOG = Logger.getLogger(LangChain4jProducer.class);

    @ConfigProperty(name = "langchain4j.store.path")
    String storePath;

    @Produces

    @Singleton
    public EmbeddingModel embeddingModel() {
        LOG.info("Using local embedding model: all-MiniLM-L6-v2-quantized (384 dim)");
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @Produces
    @Singleton
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        Path path = Path.of(storePath);
        InMemoryEmbeddingStore<TextSegment> store = loadOrCreate(path);

        final Path finalPath = path;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.createDirectories(finalPath.getParent());
                Files.writeString(finalPath, store.serializeToJson());
                LOG.infof("Saved embedding store to disk: %s (%d entries)", finalPath, store.size());
            } catch (IOException e) {
                LOG.errorf("Failed to save store: %s", e.getMessage());
            }
        }));

        return store;
    }

    private InMemoryEmbeddingStore<TextSegment> loadOrCreate(Path path) {
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromJson(json);
                LOG.infof("Loaded embedding store from disk: %s (%d entries)", path, store.size());
                return store;
            } catch (IOException e) {
                LOG.warnf("Failed to load store from %s, creating new: %s", path, e.getMessage());
            }
        }
        LOG.infof("Created new embedding store (will persist to: %s)", path);
        return new InMemoryEmbeddingStore<>();
    }
}
