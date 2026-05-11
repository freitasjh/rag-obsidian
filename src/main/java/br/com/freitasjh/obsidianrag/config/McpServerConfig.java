package br.com.freitasjh.obsidianrag.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "mcp.server")
public interface McpServerConfig {

    @WithDefault("obsidian-rag")
    String name();

    @WithDefault("1.0.0")
    String version();
}
