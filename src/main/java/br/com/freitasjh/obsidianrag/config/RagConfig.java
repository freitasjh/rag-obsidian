package br.com.freitasjh.obsidianrag.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "rag")
public interface RagConfig {

    @WithName("chunk-size")
    @WithDefault("1000")
    int chunkSize();

    @WithName("chunk-overlap")
    @WithDefault("200")
    int chunkOverlap();

    @WithName("max-results")
    @WithDefault("10")
    int maxResults();
}
