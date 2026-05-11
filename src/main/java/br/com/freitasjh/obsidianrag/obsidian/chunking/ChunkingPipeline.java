package br.com.freitasjh.obsidianrag.obsidian.chunking;

import br.com.freitasjh.obsidianrag.config.RagConfig;
import br.com.freitasjh.obsidianrag.model.Chunk;
import br.com.freitasjh.obsidianrag.model.ChunkMetadata;
import br.com.freitasjh.obsidianrag.model.ParsedMarkdown;
import br.com.freitasjh.obsidianrag.model.VaultDocument;
import br.com.freitasjh.obsidianrag.obsidian.parser.MarkdownParser;
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

    public List<Chunk> process(VaultDocument document) {
        List<Chunk> chunks = new ArrayList<>();

        ParsedMarkdown parsed = markdownParser.parse(document.getContent());

        List<ChunkContent> sections = splitByHeadings(parsed, document);

        for (int i = 0; i < sections.size(); i++) {
            ChunkContent section = sections.get(i);

            if (section.content().length() <= ragConfig.chunkSize()) {
                Chunk chunk = createChunk(document, section, i, sections.size());
                chunks.add(chunk);
            } else {
                List<Chunk> subChunks = splitBySize(section, document, i, sections.size());
                chunks.addAll(subChunks);
            }
        }

        LOG.infof("Created %d chunks from document: %s", chunks.size(), document.getPath());
        return chunks;
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

    private List<Chunk> splitBySize(ChunkContent section, VaultDocument document, int sectionIndex, int totalSections) {
        List<Chunk> chunks = new ArrayList<>();
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
                ChunkMetadata metadata = createMetadata(document, section.heading(), section.parentHeading());
                metadata.setChunkIndex(sectionIndex * 100 + subIndex);
                metadata.setTotalChunks(totalSections * 100);

                Chunk chunk = new Chunk();
                chunk.setId(document.getPath() + "_chunk_" + sectionIndex + "_" + subIndex);
                chunk.setContent(chunkText);
                chunk.setMetadata(metadata);

                chunks.add(chunk);
            }

            start = end - overlap;
            if (start <= chunks.getLast().getContent().length()) {
                break;
            }
            subIndex++;
        }

        return chunks;
    }

    private Chunk createChunk(VaultDocument document, ChunkContent section, int index, int total) {
        ChunkMetadata metadata = createMetadata(document, section.heading(), section.parentHeading());
        metadata.setChunkIndex(index);
        metadata.setTotalChunks(total);

        Chunk chunk = new Chunk();
        chunk.setId(document.getPath() + "_chunk_" + index);
        chunk.setContent(section.content());
        chunk.setMetadata(metadata);

        return chunk;
    }

    private ChunkMetadata createMetadata(VaultDocument document, String heading, String parentHeading) {
        ParsedMarkdown parsed = markdownParser.parse(document.getContent());

        ChunkMetadata metadata = new ChunkMetadata();
        metadata.setSource(document.getPath());
        metadata.setHeading(heading);
        metadata.setParentHeading(parentHeading);
        metadata.setTags(parsed.getTags());
        metadata.setVault(document.getMetadata().get("vault").toString());
        metadata.setLinks(parsed.getWikiLinks());
        metadata.setFrontmatter(parsed.getFrontmatter());

        return metadata;
    }

}
