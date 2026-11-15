package lk.swiftdrop.ai.knowledge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Loads the help articles into the in-memory vector store at startup. It runs before the service
 * reports itself ready, so the readiness probe stays down until the knowledge base is loaded.
 */
public class KnowledgeIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestor.class);

    private final ResourcePatternResolver resources;
    private final VectorStore vectorStore;
    private final KnowledgeProperties properties;

    public KnowledgeIngestor(ResourcePatternResolver resources, VectorStore vectorStore,
            KnowledgeProperties properties) {
        this.resources = resources;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        long started = System.nanoTime();
        List<Document> chunks = load();
        if (chunks.isEmpty()) {
            log.warn("No help articles found at {}", properties.location());
            return;
        }
        vectorStore.add(chunks);
        log.info("Knowledge base loaded: {} chunks in {} ms", chunks.size(), (System.nanoTime() - started) / 1_000_000);
    }

    List<Document> load() {
        try {
            List<Document> chunks = new ArrayList<>();
            for (Resource resource : resources.getResources(properties.location())) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".md")) {
                    continue;
                }
                String slug = filename.substring(0, filename.length() - ".md".length());
                chunks.addAll(MarkdownChunker.chunk(slug, resource.getContentAsString(StandardCharsets.UTF_8)));
            }
            return chunks;
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read help articles at " + properties.location(), ex);
        }
    }
}
