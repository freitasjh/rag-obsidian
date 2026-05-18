package br.com.freitasjh.obsidianrag.obsidian.chunking;

import br.com.freitasjh.obsidianrag.config.RagConfig;
import br.com.freitasjh.obsidianrag.model.ParsedMarkdown;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.obsidian.parser.MarkdownParser;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ChunkingPipeline {

    private static final Logger LOG = Logger.getLogger(ChunkingPipeline.class);

    @Inject
    MarkdownParser markdownParser;

    @Inject
    RagConfig ragConfig;

    public List<TextSegment> process(VaultDocument document) {
        List<TextSegment> segments = new ArrayList<>();

        ParsedMarkdown parsed = markdownParser.parse(document.getContent());
        List<ChunkContent> sections = splitByHeadings(parsed, document);

        for (int i = 0; i < sections.size(); i++) {
            ChunkContent section = sections.get(i);

            if (section.content().length() <= ragConfig.chunkSize()) {
                TextSegment segment = createSegment(document, parsed, section, i, sections.size());
                segments.add(segment);
            } else {
                List<TextSegment> subSegments = splitBySize(document, parsed, section, i, sections.size());
                segments.addAll(subSegments);
            }
        }

        LOG.infof("Created %d segments from document: %s", segments.size(), document.getPath());
        return segments;
    }

    private List<ChunkContent> splitByHeadings(ParsedMarkdown parsed, VaultDocument document) {
        List<ChunkContent> sections = new ArrayList<>();
        List<ParsedMarkdown.Heading> headings = parsed.getHeadings();
        String content = parsed.getContentWithoutFrontmatter();
        String[] lines = content.split("\n");

        if (headings.isEmpty()) {
            sections.add(new ChunkContent(content, null, null));
            return sections;
        }

        int currentLine = 0;
        String currentHeading = null;
        String parentHeading = null;
        StringBuilder currentContent = new StringBuilder();

        for (ParsedMarkdown.Heading heading : headings) {
            if (heading.getLineNumber() > currentLine) {
                if (!currentContent.isEmpty()) {
                    sections.add(new ChunkContent(
                            currentContent.toString().trim(),
                            currentHeading,
                            parentHeading
                    ));
                }

                currentHeading = heading.getText();
                parentHeading = findParentHeading(headings, heading);
                currentContent = new StringBuilder();

                for (int i = currentLine; i < heading.getLineNumber() - 1 && i < lines.length; i++) {
                    if (!lines[i].trim().startsWith("#")) {
                        currentContent.append(lines[i]).append("\n");
                    }
                }
                currentContent.append("# ").append(heading.getText()).append("\n");

                currentLine = heading.getLineNumber();
            }
        }

        if (!currentContent.isEmpty() || currentHeading != null) {
            for (int i = currentLine; i < lines.length; i++) {
                if (!lines[i].trim().startsWith("#")) {
                    currentContent.append(lines[i]).append("\n");
                }
            }
            sections.add(new ChunkContent(
                    currentContent.toString().trim(),
                    currentHeading,
                    parentHeading
            ));
        }

        return sections;
    }

    private String findParentHeading(List<ParsedMarkdown.Heading> headings, ParsedMarkdown.Heading current) {
        for (ParsedMarkdown.Heading heading : headings) {
            if (heading.getLineNumber() < current.getLineNumber() &&
                    heading.getLevel() < current.getLevel()) {
                return heading.getText();
            }
        }
        return null;
    }

    private List<TextSegment> splitBySize(VaultDocument document, ParsedMarkdown parsed,
                                          ChunkContent section, int sectionIndex, int totalSections) {
        List<TextSegment> segments = new ArrayList<>();
        String content = section.content();
        int chunkSize = ragConfig.chunkSize();
        int overlap = ragConfig.chunkOverlap();

        int start = 0;
        int subIndex = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            if (end < content.length() && end > start) {
                int lastNewline = content.lastIndexOf('\n', end);
                int lastSpace = content.lastIndexOf(' ', end);

                if (lastNewline > start + chunkSize / 2) {
                    end = lastNewline + 1;
                } else if (lastSpace > start + chunkSize / 2) {
                    end = lastSpace + 1;
                }
            }

            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                Metadata metadata = buildMetadata(document, parsed, section.heading(), section.parentHeading());
                metadata.put("chunkIndex", sectionIndex * 100 + subIndex);
                metadata.put("totalChunks", totalSections * 100);

                TextSegment segment = TextSegment.from(chunkText, metadata);
                segments.add(segment);
            }

            start = end - overlap;
            if (start <= segments.getLast().text().length()) {
                break;
            }
            subIndex++;
        }

        return segments;
    }

    private TextSegment createSegment(VaultDocument document, ParsedMarkdown parsed,
                                      ChunkContent section, int index, int total) {
        Metadata metadata = buildMetadata(document, parsed, section.heading(), section.parentHeading());
        metadata.put("chunkIndex", index);
        metadata.put("totalChunks", total);

        return TextSegment.from(section.content(), metadata);
    }

    private Metadata buildMetadata(VaultDocument document, ParsedMarkdown parsed,
                                   String heading, String parentHeading) {
        Metadata metadata = new Metadata();
        metadata.put("source", document.getPath());
        if (heading != null) metadata.put("heading", heading);
        if (parentHeading != null) metadata.put("parentHeading", parentHeading);
        metadata.put("vault", document.getMetadata().getOrDefault("vault", "").toString());

        if (parsed.getTags() != null && !parsed.getTags().isEmpty()) {
            metadata.put("tags", String.join(",", parsed.getTags()));
        }
        if (parsed.getWikiLinks() != null && !parsed.getWikiLinks().isEmpty()) {
            metadata.put("links", String.join(",", parsed.getWikiLinks()));
        }

        return metadata;
    }
}
