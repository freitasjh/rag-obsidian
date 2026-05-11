package br.com.freitasjh.obsidianrag.mcp;

import br.com.freitasjh.obsidianrag.model.SearchResult;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.service.IndexingService;
import br.com.freitasjh.obsidianrag.service.SearchService;
import br.com.freitasjh.obsidianrag.service.VaultService;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class McpToolService {

    private static final Logger LOG = Logger.getLogger(McpToolService.class);

    @Inject
    SearchService searchService;

    @Inject
    IndexingService indexingService;

    @Inject
    VaultService vaultService;

    @Tool(name = "search_notes", description = "Search through indexed notes using semantic similarity.")
    public Map<String, Object> searchNotes(
            @ToolArg(name = "query", description = "The search query", required = true) String query,
            @ToolArg(name = "limit", description = "Maximum number of results", required = false) Integer limit) {
        int limitVal = limit != null ? limit : 5;
        List<SearchResult> results = searchService.search(query, limitVal);
        return Map.of("results", results, "count", results.size(), "query", query);
    }

    @Tool(name = "refresh_index", description = "Reindex the vault in background (full or incremental).")
    public Map<String, Object> refreshIndex(
            @ToolArg(name = "full", description = "Whether to perform a full reindex", required = false) Boolean full) {
        boolean fullVal = full == null || full;
        CompletableFuture.runAsync(() -> indexingService.reindex(fullVal));
        return Map.of("status", "reindex_started", "full", fullVal, "message", "Reindex started in background. Use get_index_status to check progress.");
    }

    @Tool(name = "get_index_status", description = "Check current indexing status and results.")
    public Map<String, Object> getIndexStatus() {
        return indexingService.getIndexingStatus();
    }

    @Tool(name = "list_notes", description = "List all indexed note paths.")
    public Map<String, Object> listNotes(
            @ToolArg(name = "limit", description = "Maximum number of notes to list", required = false) Integer limit) {
        List<String> notes = vaultService.listDocumentPaths();
        int limitVal = limit != null ? limit : notes.size();
        List<String> limited = notes.stream().limit(limitVal).toList();
        return Map.of("notes", limited, "total", notes.size());
    }

    @Tool(name = "get_note", description = "Retrieve full content of a note by its relative path.")
    public Map<String, Object> getNote(
            @ToolArg(name = "path", description = "Relative path of the note", required = true) String path) {
        Optional<SearchResult> doc = vaultService.getDocumentByPath(path);
        if (doc.isPresent()) {
            SearchResult r = doc.get();
            return Map.of("found", true, "content", r.getContent(), "metadata", r.getMetadata());
        }
        return Map.of("found", false, "message", "Note not found: " + path);
    }

    @Tool(name = "read_note", description = "Read raw content of a note by its relative path.")
    public Map<String, Object> readNote(
            @ToolArg(name = "path", description = "Relative path of the note (e.g. folder/note.md)", required = true) String path) {
        Optional<SearchResult> doc = vaultService.getDocumentByPath(path);
        if (doc.isPresent()) {
            return Map.of("content", doc.get().getContent());
        }
        return Map.of("error", "Note not found: " + path);
    }

    @Tool(name = "write_note", description = "Create or overwrite a note in the vault. Incrementally indexes the note after saving.")
    public Map<String, Object> writeNote(
            @ToolArg(name = "path", description = "Relative path of the note (e.g. folder/note.md)", required = true) String path,
            @ToolArg(name = "content", description = "Full markdown content of the note", required = true) String content) {
        vaultService.writeNote(path, content);
        VaultDocument doc = new VaultDocument(path, path.replace(".md", "").replace("/", " / "), content, Map.of("vault", "brain"));
        CompletableFuture.runAsync(() -> {
            indexingService.removeDocument(path);
            indexingService.indexDocument(doc);
        });
        return Map.of("status", "saved", "path", path, "message", "Note saved and indexed.");
    }
}
