package br.com.freitasjh.obsidianrag.model;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class Chunk {

    private String id;
    private String content;
    private ChunkMetadata metadata;
    private List<Double> embedding;

    public Chunk() {
    }

    public Chunk(String id, String content, ChunkMetadata metadata) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ChunkMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ChunkMetadata metadata) {
        this.metadata = metadata;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chunk chunk = (Chunk) o;
        return Objects.equals(id, chunk.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Chunk.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("content=" + (content != null ? content.substring(0, Math.min(30, content.length())) + "..." : null))
                .toString();
    }
}
