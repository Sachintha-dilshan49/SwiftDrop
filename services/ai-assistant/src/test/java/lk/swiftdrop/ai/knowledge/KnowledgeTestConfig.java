package lk.swiftdrop.ai.knowledge;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Replaces the ONNX embedding model in Spring Boot tests. Use with
 * {@code swiftdrop.ai.knowledge.embedding-provider=test} so the real model is not created.
 */
@TestConfiguration(proxyBeanMethods = false)
public class KnowledgeTestConfig {

    public static final String PROPERTY = "swiftdrop.ai.knowledge.embedding-provider=test";

    @Bean
    EmbeddingModel testEmbeddingModel() {
        return new WordHashEmbeddingModel();
    }
}
