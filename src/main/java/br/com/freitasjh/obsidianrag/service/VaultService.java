package br.com.freitasjh.obsidianrag.service;

import br.com.freitasjh.obsidianrag.config.ObsidianConfig;
import br.com.freitasjh.obsidianrag.model.SearchResult;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.obsidian.VaultLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VaultService {

    private static final Logger LOG = Logger.getLogger(VaultService.class);

    @Inject
    ObsidianConfig obsidianConfig;

    @Inject
    VaultLoader vaultLoader;

    public Optional<SearchResult> getDocumentByPath(String path) {
        for (VaultDocument doc : vaultLoader.loadVault()) {
            if (doc.getPath().equals(path)) {
                return Optional.of(new SearchResult(
                        doc.getContent(),
                        1.0,
                        doc.getMetadata()
                ));
            }
        }
        return Optional.empty();
    }

    public List<String> listDocumentPaths() {
        List<VaultDocument> docs = vaultLoader.loadVault();
        return docs.stream()
                .map(VaultDocument::getPath)
                .sorted()
                .toList();
    }

    public void writeNote(String path, String content) {
        Path fullPath = obsidianConfig.vault().path().resolve(path);
        try {
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, content);
            LOG.infof("Note written: %s", fullPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write note: " + path, e);
        }
    }
}
