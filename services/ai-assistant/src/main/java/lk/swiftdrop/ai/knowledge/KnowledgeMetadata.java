package lk.swiftdrop.ai.knowledge;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lk.swiftdrop.ai.chat.dto.SourceRef;
import org.springframework.ai.document.Document;

/** Metadata stored on every knowledge-base chunk. */
public final class KnowledgeMetadata {

    /** Article slug: the file name without {@code .md}, e.g. {@code prohibited-items}. */
    public static final String ARTICLE = "article";
    /** Heading of the section the chunk comes from. */
    public static final String SECTION = "section";

    private KnowledgeMetadata() {
    }

    /** Distinct sources of the given chunks, in order. */
    public static List<SourceRef> sources(Collection<Document> documents) {
        return documents.stream()
                .map(doc -> new SourceRef(string(doc, ARTICLE), string(doc, SECTION)))
                .filter(source -> source.article() != null)
                .distinct()
                .toList();
    }

    private static String string(Document document, String key) {
        return Objects.toString(document.getMetadata().get(key), null);
    }
}
