package br.com.freitasjh.obsidianrag.obsidian.parser;

import br.com.freitasjh.obsidianrag.model.ParsedMarkdown;
import br.com.freitasjh.obsidianrag.model.ParsedMarkdown.Heading;
import jakarta.enterprise.context.ApplicationScoped;
import org.commonmark.parser.Parser;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class MarkdownParser {

    private static final Logger LOG = Logger.getLogger(MarkdownParser.class);
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\n([\\s\\S]*?)\n---", Pattern.MULTILINE);
    private static final Pattern TAG_PATTERN = Pattern.compile("#([a-zA-Z0-9_-]+)");
    private static final Pattern WIKI_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]|]+)(?:\\|[^\\]]+)?\\]\\]");

    private final Parser parser;

    public MarkdownParser() {
        this.parser = Parser.builder().build();
    }

    public ParsedMarkdown parse(String content) {
        ParsedMarkdown result = new ParsedMarkdown();

        Map<String, Object> frontmatter = extractFrontmatter(content);
        result.setFrontmatter(frontmatter);

        String contentWithoutFrontmatter = removeFrontmatter(content);
        result.setContentWithoutFrontmatter(contentWithoutFrontmatter);

        List<Heading> headings = extractHeadings(contentWithoutFrontmatter);
        result.setHeadings(headings);

        List<String> tags = extractTags(contentWithoutFrontmatter);
        result.setTags(tags);

        List<String> wikiLinks = extractWikiLinks(contentWithoutFrontmatter);
        result.setWikiLinks(wikiLinks);

        String title = extractTitleFromFrontmatterOrContent(frontmatter, contentWithoutFrontmatter);
        result.setTitle(title);

        return result;
    }

    private Map<String, Object> extractFrontmatter(String content) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            String yamlContent = matcher.group(1);
            try {
                Yaml yaml = new Yaml();
                Map<String, Object> frontmatter = yaml.load(yamlContent);
                return frontmatter != null ? frontmatter : new HashMap<>();
            } catch (Exception e) {
                LOG.warnf("Failed to parse frontmatter: %s", e.getMessage());
            }
        }
        return new HashMap<>();
    }

    private String removeFrontmatter(String content) {
        return content.replaceFirst(FRONTMATTER_PATTERN.pattern(), "").trim();
    }

    private List<Heading> extractHeadings(String content) {
        List<Heading> headings = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("#")) {
                int level = 0;
                for (char c : line.toCharArray()) {
                    if (c == '#') {
                        level++;
                    } else {
                        break;
                    }
                }
                if (level > 0 && line.length() > level && line.charAt(level) == ' ') {
                    String text = line.substring(level + 1).trim();
                    headings.add(new Heading(level, text, i + 1));
                }
            }
        }

        return headings;
    }

    private List<String> extractTags(String content) {
        Set<String> tags = new LinkedHashSet<>();
        Matcher matcher = TAG_PATTERN.matcher(content);

        while (matcher.find()) {
            String tag = matcher.group(1);
            if (!tag.matches("\\d+")) {
                tags.add(tag.toLowerCase());
            }
        }

        if (content.contains("tags:")) {
            String tagsSection = content.substring(content.indexOf("tags:"));
            int endIndex = Math.min(tagsSection.indexOf("\n", 10), tagsSection.length());
            String tagsLine = tagsSection.substring(6, endIndex).trim();
            if (tagsLine.startsWith("[")) {
                String[] tagArray = tagsLine.substring(1, tagsLine.indexOf("]")).split(",");
                for (String tag : tagArray) {
                    tags.add(tag.trim().replace("\"", "").replace("'", "").toLowerCase());
                }
            }
        }

        return new ArrayList<>(tags);
    }

    private List<String> extractWikiLinks(String content) {
        List<String> links = new ArrayList<>();
        Matcher matcher = WIKI_LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            String link = matcher.group(1);
            links.add(link);
        }

        return links;
    }

    private String extractTitleFromFrontmatterOrContent(Map<String, Object> frontmatter, String content) {
        if (frontmatter.containsKey("title")) {
            return frontmatter.get("title").toString();
        }

        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ") && !line.startsWith("# ") && line.length() > 2) {
                return line.substring(2).trim();
            }
        }

        return "Untitled";
    }
}
