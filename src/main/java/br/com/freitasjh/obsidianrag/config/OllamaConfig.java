package br.com.freitasjh.obsidianrag.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "ollama")
public interface OllamaConfig {

    @WithDefault("http://localhost:11434")
    String baseUrl();

    @WithName("embedding-model")
    @WithDefault("nomic-embed-text")
    String embeddingModel();
}
