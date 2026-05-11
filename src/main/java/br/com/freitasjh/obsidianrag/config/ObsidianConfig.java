package br.com.freitasjh.obsidianrag.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "obsidian")
public interface ObsidianConfig {

    @WithDefault("/vault")
    VaultConfig vault();

    interface VaultConfig {
        @WithDefault(".")
        java.nio.file.Path path();

        @WithDefault("main")
        String name();
    }
}
