package br.com.freitasjh.obsidianrag.obsidian;

import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.config.ObsidianConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class VaultLoader {

    private static final Logger LOG = Logger.getLogger(VaultLoader.class);
    private static final String OBSIDIAN_DIR = ".obsidian";
    private static final String MARKDOWN_EXTENSION = ".md";

    @Inject
    ObsidianConfig config;

    public List<VaultDocument> loadVault() {
        List<VaultDocument> documents = new ArrayList<>();
        Path vaultPath = config.vault().path();

        LOG.infof("Loading vault from: %s", vaultPath);

        if (!Files.exists(vaultPath)) {
            LOG.warnf("Vault path does not exist: %s", vaultPath);
            return documents;
        }

        if (!Files.isDirectory(vaultPath)) {
            LOG.warnf("Vault path is not a directory: %s", vaultPath);
            return documents;
        }

        try (Stream<Path> pathStream = Files.walk(vaultPath)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(MARKDOWN_EXTENSION))
                    .filter(path -> !isInObsidianDir(path, vaultPath))
                    .forEach(path -> {
                        try {
                            VaultDocument doc = loadDocument(path, vaultPath);
                            documents.add(doc);
                        } catch (Exception e) {
                            LOG.warnf("Failed to load document: %s - %s", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOG.errorf("Error walking vault directory: %s", e.getMessage());
        }

        LOG.infof("Loaded %d documents from vault", documents.size());
        return documents;
    }

    private boolean isInObsidianDir(Path path, Path vaultPath) {
        return path.toString().contains(vaultPath.resolve(OBSIDIAN_DIR).toString());
    }

    private VaultDocument loadDocument(Path path, Path vaultPath) throws IOException {
        String content = Files.readString(path);
        String relativePath = vaultPath.relativize(path).toString();
        String title = extractTitle(path.getFileName().toString(), content);

        VaultDocument document = new VaultDocument();
        document.setPath(relativePath);
        document.setTitle(title);
        document.setContent(content);
        document.setMetadata(Map.of(
                "vault", config.vault().name(),
                "fullPath", path.toAbsolutePath().toString(),
                "lastModified", Files.getLastModifiedTime(path).toMillis()
        ));

        return document;
    }

    private String extractTitle(String filename, String content) {
        String title = filename.replace(MARKDOWN_EXTENSION, "");

        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
                break;
            }
        }

        return title;
    }

    public long countMarkdownFiles() {
        Path vaultPath = config.vault().path();
        if (!Files.exists(vaultPath)) {
            return 0;
        }

        try (Stream<Path> pathStream = Files.walk(vaultPath)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(MARKDOWN_EXTENSION))
                    .filter(path -> !isInObsidianDir(path, vaultPath))
                    .count();
        } catch (IOException e) {
            LOG.errorf("Error counting markdown files: %s", e.getMessage());
            return 0;
        }
    }
}
