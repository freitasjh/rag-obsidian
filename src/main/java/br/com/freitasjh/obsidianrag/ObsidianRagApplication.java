package br.com.freitasjh.obsidianrag;

import java.util.Arrays;
import java.util.Map;

import br.com.freitasjh.obsidianrag.service.IndexingService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@QuarkusMain
public class ObsidianRagApplication implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(ObsidianRagApplication.class);

    private static final Map<String, String> SHORTHAND = Map.of(
        "server-port", "quarkus.http.port",
        "vault", "obsidian.vault.path",
        "vault-name", "obsidian.vault.name",
        "chunk-size", "rag.chunk-size",
        "chunk-overlap", "rag.chunk-overlap",
        "max-results", "rag.max-results",
        "embedding-store", "langchain4j.store.path"
    );

    @Inject
    IndexingService indexingService;

    public static void main(String[] args) {
        Quarkus.run(ObsidianRagApplication.class, parseAndApplyProperties(args));
    }

    static String[] parseAndApplyProperties(String[] args) {
        if (args == null) return new String[0];
        return Arrays.stream(args)
            .filter(arg -> {
                if (arg.startsWith("-D")) {
                    String kv = arg.substring(2);
                    int eq = kv.indexOf('=');
                    if (eq > 0) {
                        String key = kv.substring(0, eq);
                        String value = kv.substring(eq + 1);
                        String mappedKey = SHORTHAND.getOrDefault(key, key);
                        System.setProperty(mappedKey, value);
                        LOG.debugf("Set config: %s=%s", mappedKey, value);
                    }
                    return false;
                }
                return true;
            })
            .toArray(String[]::new);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("=".repeat(60));
        LOG.info("  Obsidian RAG MCP Server");
        LOG.info("=".repeat(60));

        LOG.info("Starting initial vault indexing...");
        indexingService.reindex(true);

        int port = Integer.getInteger("quarkus.http.port", 8087);
        LOG.info("=".repeat(60));
        LOG.info("  Obsidian RAG is ready!");
        LOG.info("  Indexing status: " + indexingService.getIndexingStatus());
        LOG.info("  HTTP API:      http://localhost:" + port + "/api");
        LOG.info("  MCP endpoint:  http://localhost:" + port + "/sse");
        LOG.info("=".repeat(60));

        Thread.currentThread().join();
        return 0;
    }
}
