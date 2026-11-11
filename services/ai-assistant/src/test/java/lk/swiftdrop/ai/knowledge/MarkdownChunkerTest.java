package lk.swiftdrop.ai.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class MarkdownChunkerTest {

    @Test
    void oneChunkPerSectionWithTitleAndSectionMetadata() {
        List<Document> chunks = MarkdownChunker.chunk("prohibited-items", """
                # Prohibited items

                ## Items you cannot send
                Weapons and explosives.

                ## Liquids
                Perfume is flammable.
                """);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1).getText()).startsWith("Prohibited items - Liquids").contains("Perfume");
        assertThat(chunks.get(1).getMetadata())
                .containsEntry(KnowledgeMetadata.ARTICLE, "prohibited-items")
                .containsEntry(KnowledgeMetadata.SECTION, "Liquids");
        assertThat(chunks).extracting(Document::getId).containsExactly("prohibited-items#0", "prohibited-items#1");
    }

    @Test
    void textBeforeFirstSectionUsesTitleAndEmptySectionsAreSkipped() {
        List<Document> chunks = MarkdownChunker.chunk("intro", """
                # Welcome
                Intro text.

                ## Empty

                ## Filled
                Content.
                """);

        assertThat(chunks).extracting(doc -> doc.getMetadata().get(KnowledgeMetadata.SECTION))
                .containsExactly("Welcome", "Filled");
    }

    @Test
    void articleWithoutTitleUsesSlug() {
        assertThat(MarkdownChunker.chunk("faq", "Just text.").getFirst().getText()).startsWith("faq");
    }
}
