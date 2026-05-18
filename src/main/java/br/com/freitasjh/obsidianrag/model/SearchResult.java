package br.com.freitasjh.obsidianrag.model;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class SearchResult {

    private String content;
    private Double score;
    private Map<String, Object> metadata;

    public SearchResult() { }

    public SearchResult(String content, Double score, Map<String, Object> metadata) {
        this.content = content;
        this.score = score;
        this.metadata = metadata;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return Objects.equals(content, that.content) && Objects.equals(score, that.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, score);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SearchResult.class.getSimpleName() + "[", "]")
                .add("content=" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : null))
                .add("score=" + score)
                .toString();
    }
}
