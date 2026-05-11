package br.com.freitasjh.obsidianrag.rag.retriever;

import br.com.freitasjh.obsidianrag.model.SearchResult;
import java.util.List;

public interface SemanticRetriever {

    List<SearchResult> search(String query, int limit);

    void index(String id, String content, java.util.Map<String, Object> metadata);

    void remove(String id);

    void clear();

    int count();
}
