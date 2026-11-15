package lk.swiftdrop.ai.knowledge;

import java.util.List;
import lk.swiftdrop.ai.tools.ToolCaller;
import lk.swiftdrop.ai.tools.ToolFailureException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Knowledge-base search as a tool, for MCP clients. In the chat the same search runs automatically
 * as a RAG advisor, so this tool is not given to the chat model.
 */
public class HelpArticleTools {

    public static final String SEARCH_HELP_ARTICLES = "search_help_articles";

    private final VectorStore vectorStore;
    private final KnowledgeProperties properties;

    public HelpArticleTools(VectorStore vectorStore, KnowledgeProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Tool(name = SEARCH_HELP_ARTICLES, description = """
            Search SwiftDrop's help articles: fees, parcel sizes and weight limits, prohibited items, \
            delivery times, statuses, failed deliveries and the merchant API. Returns matching passages \
            with the article and section they come from.""")
    public List<HelpPassage> searchHelpArticles(
            @ToolParam(description = "The question or keywords, e.g. can I send perfume") String query,
            ToolContext toolContext) {
        ToolCaller.begin(toolContext, SEARCH_HELP_ARTICLES);
        if (query == null || query.isBlank() || query.length() > 500) {
            throw new ToolFailureException("Give a short question or keywords to search for.");
        }
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query.trim())
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .build());
        return documents.stream()
                .map(doc -> new HelpPassage(
                        String.valueOf(doc.getMetadata().get(KnowledgeMetadata.ARTICLE)),
                        String.valueOf(doc.getMetadata().get(KnowledgeMetadata.SECTION)),
                        doc.getText()))
                .toList();
    }

    /** One matching passage of a help article. */
    public record HelpPassage(String article, String section, String text) {
    }
}
