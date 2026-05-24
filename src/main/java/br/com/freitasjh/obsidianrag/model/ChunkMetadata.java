package br.com.freitasjh.obsidianrag.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class ChunkMetadata {

    private String source;
    private String heading;
    private String parentHeading;
    private List<String> tags;
    private String vault;
    private int chunkIndex;
    private int totalChunks;
    private List<String> links;
    private Map<String, Object> frontmatter;

    public ChunkMetadata() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getParentHeading() {
        return parentHeading;
    }

    public void setParentHeading(String parentHeading) {
        this.parentHeading = parentHeading;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getVault() {
        return vault;
    }

    public void setVault(String vault) {
        this.vault = vault;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public Map<String, Object> getFrontmatter() {
        return frontmatter;
    }

    public void setFrontmatter(Map<String, Object> frontmatter) {
        this.frontmatter = frontmatter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkMetadata that = (ChunkMetadata) o;
        return Objects.equals(source, that.source) && Objects.equals(chunkIndex, that.chunkIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, chunkIndex);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChunkMetadata.class.getSimpleName() + "[", "]")
                .add("source=" + source)
                .add("heading=" + heading)
                .add("vault=" + vault)
                .toString();
    }
}
