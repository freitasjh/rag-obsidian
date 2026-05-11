package br.com.freitasjh.obsidianrag.tools;

import br.com.freitasjh.obsidianrag.model.SearchResult;
import br.com.freitasjh.obsidianrag.service.IndexingService;
import br.com.freitasjh.obsidianrag.service.SearchService;
import br.com.freitasjh.obsidianrag.service.VaultService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestApiResource {

    @Inject
    SearchService searchService;

    @Inject
    VaultService vaultService;

    @Inject
    IndexingService indexingService;

    @GET
    @Path("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "obsidian-rag",
                "indexedChunks", indexingService.getIndexedChunkCount()
        );
    }

    @GET
    @Path("/search")
    public Response search(
            @QueryParam("q") String query,
            @QueryParam("limit") @DefaultValue("5") int limit
    ) {
        if (query == null || query.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Query parameter 'q' is required"))
                    .build();
        }

        List<SearchResult> results = searchService.search(query, limit);

        return Response.ok(Map.of(
                "query", query,
                "results", results,
                "count", results.size()
        )).build();
    }

    @GET
    @Path("/notes")
    public Map<String, Object> listNotes(@QueryParam("limit") @DefaultValue("50") int limit) {
        List<String> paths = vaultService.listDocumentPaths();
        return Map.of(
                "notes", paths.stream().limit(limit).toList(),
                "total", paths.size()
        );
    }

    @GET
    @Path("/notes/{path:.*}")
    public Response getNote(@PathParam("path") String path) {
        return vaultService.getDocumentByPath(path)
                .map(doc -> Response.ok(Map.of(
                        "found", true,
                        "content", doc.getContent(),
                        "metadata", doc.getMetadata()
                )).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("found", false, "message", "Note not found"))
                        .build());
    }

    @POST
    @Path("/reindex")
    public Map<String, Object> reindex(@QueryParam("full") @DefaultValue("true") boolean full) {
        indexingService.reindex(full);
        return Map.of(
                "status", "completed",
                "indexedChunks", indexingService.getIndexedChunkCount()
        );
    }

    @GET
    @Path("/status")
    public Map<String, Object> status() {
        return indexingService.getIndexingStatus();
    }
}
