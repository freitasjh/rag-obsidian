package br.com.freitasjh.obsidianrag;

import br.com.freitasjh.obsidianrag.model.ParsedMarkdown;
import br.com.freitasjh.obsidianrag.obsidian.parser.MarkdownParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownParserTest {

    private MarkdownParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownParser();
    }

    @Test
    void shouldExtractFrontmatter() {
        String content = """
                ---
                title: Test Note
                tags: [test, sample]
                ---
                # Hello World
                """;

        ParsedMarkdown result = parser.parse(content);

        assertThat(result.getFrontmatter()).containsKey("title");
        assertThat(result.getFrontmatter().get("title")).isEqualTo("Test Note");
    }

    @Test
    void shouldExtractHeadings() {
        String content = """
                # Main Heading
                Some content here
                ## Sub Heading
                More content
                """;

        ParsedMarkdown result = parser.parse(content);

        assertThat(result.getHeadings()).hasSize(2);
        assertThat(result.getHeadings().get(0).getText()).isEqualTo("Main Heading");
        assertThat(result.getHeadings().get(1).getText()).isEqualTo("Sub Heading");
    }

    @Test
    void shouldExtractTags() {
        String content = """
                # Test
                #tag1 some text
                #tag-two more text
                """;

        ParsedMarkdown result = parser.parse(content);

        assertThat(result.getTags()).contains("tag1", "tag-two");
    }

    @Test
    void shouldExtractWikiLinks() {
        String content = """
                # Test
                Check [[Link One]] and [[Link Two|display text]]
                """;

        ParsedMarkdown result = parser.parse(content);

        assertThat(result.getWikiLinks()).containsExactlyInAnyOrder("Link One", "Link Two");
    }

    @Test
    void shouldHandleEmptyContent() {
        ParsedMarkdown result = parser.parse("");

        assertThat(result.getContentWithoutFrontmatter()).isEmpty();
        assertThat(result.getFrontmatter()).isEmpty();
        assertThat(result.getHeadings()).isEmpty();
    }
}
