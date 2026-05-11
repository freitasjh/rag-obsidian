package br.com.freitasjh.obsidianrag.model;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class VaultDocument {

    private String path;
    private String title;
    private String content;
    private Map<String, Object> metadata;

    public VaultDocument() {
    }

    public VaultDocument(String path, String title, String content, Map<String, Object> metadata) {
        this.path = path;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultDocument that = (VaultDocument) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", VaultDocument.class.getSimpleName() + "[", "]")
                .add("path=" + path)
                .add("title=" + title)
                .toString();
    }
}
