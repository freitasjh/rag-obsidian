package br.com.freitasjh.obsidianrag.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class ParsedMarkdown {

    private String contentWithoutFrontmatter;
    private Map<String, Object> frontmatter;
    private List<Heading> headings;
    private List<String> tags;
    private List<String> wikiLinks;
    private String title;

    public ParsedMarkdown() { }

    public ParsedMarkdown(String contentWithoutFrontmatter, Map<String, Object> frontmatter,
                          List<Heading> headings, List<String> tags, List<String> wikiLinks, String title) {
        this.contentWithoutFrontmatter = contentWithoutFrontmatter;
        this.frontmatter = frontmatter;
        this.headings = headings;
        this.tags = tags;
        this.wikiLinks = wikiLinks;
        this.title = title;
    }

    public String getContentWithoutFrontmatter() { return contentWithoutFrontmatter; }
    public void setContentWithoutFrontmatter(String contentWithoutFrontmatter) { this.contentWithoutFrontmatter = contentWithoutFrontmatter; }
    public Map<String, Object> getFrontmatter() { return frontmatter; }
    public void setFrontmatter(Map<String, Object> frontmatter) { this.frontmatter = frontmatter; }
    public List<Heading> getHeadings() { return headings; }
    public void setHeadings(List<Heading> headings) { this.headings = headings; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getWikiLinks() { return wikiLinks; }
    public void setWikiLinks(List<String> wikiLinks) { this.wikiLinks = wikiLinks; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedMarkdown that = (ParsedMarkdown) o;
        return Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParsedMarkdown.class.getSimpleName() + "[", "]")
                .add("title=" + title)
                .add("headings=" + headings)
                .toString();
    }

    public static class Heading {
        private int level;
        private String text;
        private int lineNumber;

        public Heading() { }
        public Heading(int level, String text, int lineNumber) {
            this.level = level;
            this.text = text;
            this.lineNumber = lineNumber;
        }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        @Override
        public String toString() {
            return "H" + level + ": " + text + " (line " + lineNumber + ")";
        }
    }
}
