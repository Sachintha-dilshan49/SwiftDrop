package lk.swiftdrop.ai.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/**
 * Splits a help article into one chunk per {@code ## } section. Articles are short, so a section is
 * a good retrieval unit: it is small enough to be specific and is cited by its heading.
 *
 * <p>Each chunk starts with the article title and section heading, so the embedding knows what the
 * section is about even when its text does not repeat it.
 */
public final class MarkdownChunker {

    private MarkdownChunker() {
    }

    /**
     * @param slug     article id, the file name without {@code .md}
     * @param markdown article text: an optional {@code # Title}, then {@code ## Section} blocks
     */
    public static List<Document> chunk(String slug, String markdown) {
        String title = slug;
        String section = null;
        StringBuilder body = new StringBuilder();
        List<Document> chunks = new ArrayList<>();

        for (String line : markdown.split("\\R")) {
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                add(chunks, slug, title, section, body);
                section = line.substring(3).trim();
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        add(chunks, slug, title, section, body);
        return chunks;
    }

    private static void add(List<Document> chunks, String slug, String title, String section, StringBuilder body) {
        String text = body.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        String heading = section == null ? title : title + " - " + section;
        String id = slug + "#" + chunks.size();
        chunks.add(Document.builder()
                .id(id)
                .text(heading + "\n\n" + text)
                .metadata(Map.of(
                        KnowledgeMetadata.ARTICLE, slug,
                        KnowledgeMetadata.SECTION, section == null ? title : section))
                .build());
    }
}
