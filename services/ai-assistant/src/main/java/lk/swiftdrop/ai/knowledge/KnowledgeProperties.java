package lk.swiftdrop.ai.knowledge;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code swiftdrop.ai.knowledge.*}: the help knowledge base used for RAG.
 *
 * @param enabled             whether articles are loaded and retrieved
 * @param location            Markdown articles, e.g. {@code classpath:knowledge/*.md}
 * @param topK                passages added to a prompt
 * @param similarityThreshold minimum cosine similarity of a passage (0 to 1)
 * @param embeddingProvider   {@code local}: ONNX all-MiniLM-L6-v2 in-process; anything else: provide
 *                            an {@code EmbeddingModel} bean yourself (tests do)
 * @param embeddingCacheDir   where the ONNX model is cached; empty for the library default
 */
@Validated
@ConfigurationProperties("swiftdrop.ai.knowledge")
public record KnowledgeProperties(
        boolean enabled,
        @NotBlank String location,
        @Min(1) @Max(10) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
        @NotBlank String embeddingProvider,
        String embeddingCacheDir) {
}
