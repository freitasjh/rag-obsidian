package br.com.freitasjh.obsidianrag.rag.embeddings;

import br.com.freitasjh.obsidianrag.config.OllamaConfig;
import br.com.freitasjh.obsidianrag.model.Chunk;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.obsidian.VaultLoader;
import br.com.freitasjh.obsidianrag.obsidian.chunking.ChunkingPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private static final Logger LOG = Logger.getLogger(OllamaEmbeddingProvider.class);

    @Inject
    OllamaConfig config;

    @Inject
    VaultLoader vaultLoader;

    @Inject
    ChunkingPipeline chunkingPipeline;

    private final Map<String, List<Double>> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    @Override
    public Embedding embed(String text) {
        if (cache.containsKey(text)) {
            return new Embedding(cache.get(text), text);
        }

        String normalizedText = text.trim();
        if (cache.containsKey(normalizedText)) {
            return new Embedding(cache.get(normalizedText), text);
        }

        List<Double> embedding = generateEmbedding(normalizedText);
        cache.put(normalizedText, embedding);
        cache.put(text, embedding);

        return new Embedding(embedding, text);
    }

    @Override
    public List<Embedding> embedAll(List<String> texts) {
        List<Embedding> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int dimensions() {
        return 768;
    }

    private final com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private List<Double> generateEmbedding(String text) {
        try {
            String apiUrl = config.baseUrl() + "/api/embed";

            String jsonBody = jsonMapper.writeValueAsString(Map.of(
                    "model", config.embeddingModel(),
                    "input", text
            ));

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    return parseEmbeddingResponse(response.toString());
                }
            } else {
                LOG.warnf("Ollama API returned status code: %d", responseCode);
                return generateFallbackEmbedding(text);
            }
        } catch (Exception e) {
            LOG.errorf("Error generating embedding: %s", e.getMessage());
            return generateFallbackEmbedding(text);
        }
    }

    private List<Double> parseEmbeddingResponse(String response) {
        List<Double> embedding = new ArrayList<>();
        try {
            var json = jsonMapper.readTree(response);
            var embeddingsArray = json.get("embeddings");
            if (embeddingsArray != null && embeddingsArray.isArray() && embeddingsArray.size() > 0) {
                var first = embeddingsArray.get(0);
                if (first.isArray()) {
                    for (var val : first) {
                        embedding.add(val.asDouble());
                    }
                }
            }
        } catch (Exception e) {
            LOG.warnf("Failed to parse embedding response: %s", e.getMessage());
        }
        return embedding.isEmpty() ? generateFallbackEmbedding("") : embedding;
    }

    private List<Double> generateFallbackEmbedding(String text) {
        int dimensions = dimensions();
        List<Double> embedding = new ArrayList<>(dimensions);
        Random random = new Random(text.hashCode());

        double magnitude = 0;
        double[] raw = new double[dimensions];

        for (int i = 0; i < dimensions; i++) {
            raw[i] = random.nextGaussian();
            magnitude += raw[i] * raw[i];
        }
        magnitude = Math.sqrt(magnitude);

        for (int i = 0; i < dimensions; i++) {
            embedding.add(raw[i] / magnitude);
        }

        return embedding;
    }

    public void preloadEmbeddings() {
        if (initialized) {
            return;
        }

        LOG.info("Preloading embeddings from vault...");
        List<VaultDocument> documents = vaultLoader.loadVault();

        for (VaultDocument doc : documents) {
            List<Chunk> chunks = chunkingPipeline.process(doc);
            for (Chunk chunk : chunks) {
                embed(chunk.getContent());
            }
        }

        initialized = true;
        LOG.infof("Preloaded %d embeddings", cache.size());
    }

    public void clearCache() {
        cache.clear();
        initialized = false;
    }

    public int cacheSize() {
        return cache.size();
    }
}
