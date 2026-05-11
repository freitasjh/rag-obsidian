package br.com.freitasjh.obsidianrag;

import br.com.freitasjh.obsidianrag.service.IndexingService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@QuarkusMain
public class ObsidianRagApplication implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(ObsidianRagApplication.class);

    @Inject
    IndexingService indexingService;

    public static void main(String[] args) {
        Quarkus.run(ObsidianRagApplication.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("=".repeat(60));
        LOG.info("  Obsidian RAG - starting...");
        LOG.info("=".repeat(60));

        LOG.info("Starting initial vault indexing...");
        indexingService.reindex(true);

        LOG.info("=".repeat(60));
        LOG.info("  Obsidian RAG is ready!");
        LOG.info("  Indexing status: " + indexingService.getIndexingStatus());
        LOG.info("=".repeat(60));

        Quarkus.waitForExit();
        return 0;
    }
}
