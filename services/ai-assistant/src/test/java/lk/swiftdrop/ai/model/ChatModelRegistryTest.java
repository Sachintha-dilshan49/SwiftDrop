package lk.swiftdrop.ai.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

class ChatModelRegistryTest {

    private final List<ChatModelRegistry.ModelSettings> built = new ArrayList<>();
    private final Function<ChatModelRegistry.ModelSettings, ChatModel> factory = settings -> {
        built.add(settings);
        return mock(ChatModel.class);
    };

    @Test
    void nullNameResolvesToDefaultModel() {
        ChatModelRegistry registry = new ChatModelRegistry(properties("groq", Map.of(
                "groq", model(true, "groq-key"),
                "openai", model(true, "openai-key"))), factory);

        assertThat(registry.resolve(null).name()).isEqualTo("groq");
        assertThat(registry.resolve("openai").name()).isEqualTo("openai");
        assertThat(built).hasSize(2);
        assertThat(built.getFirst().timeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void unknownModelNameIsRejected() {
        ChatModelRegistry registry = new ChatModelRegistry(properties("groq", Map.of(
                "groq", model(true, "key"))), factory);

        assertThatThrownBy(() -> registry.resolve("gpt-9"))
                .isInstanceOf(UnknownModelException.class)
                .hasMessageContaining("gpt-9");
    }

    @Test
    void disabledModelIsNotBuiltOrListed() {
        ChatModelRegistry registry = new ChatModelRegistry(properties("groq", Map.of(
                "groq", model(true, "key"),
                "openai", model(false, "key"))), factory);

        assertThat(built).hasSize(1);
        assertThat(registry.summaries()).extracting(ChatModelRegistry.ModelSummary::name).containsExactly("groq");
        assertThatThrownBy(() -> registry.resolve("openai")).isInstanceOf(UnknownModelException.class);
    }

    @Test
    void modelWithoutKeyIsListedButUnavailable() {
        ChatModelRegistry registry = new ChatModelRegistry(properties("groq", Map.of(
                "groq", model(true, ""))), factory);

        assertThat(built).isEmpty();
        assertThat(registry.summaries().getFirst().available()).isFalse();
        assertThatThrownBy(() -> registry.resolve(null))
                .isInstanceOf(AssistantUnavailableException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void localModelNeedsNoKey() {
        AiModelProperties.Model ollama = new AiModelProperties.Model(true, "http://localhost:11434/v1", null, false,
                "llama3.2", null);
        ChatModelRegistry registry = new ChatModelRegistry(properties("ollama", Map.of("ollama", ollama)), factory);

        assertThat(registry.resolve(null).name()).isEqualTo("ollama");
    }

    @Test
    void startupFailsWhenDefaultModelIsMissingOrDisabled() {
        assertThatThrownBy(() -> new ChatModelRegistry(properties("groq", Map.of(
                "openai", model(true, "key"))), factory))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ChatModelRegistry(properties("groq", Map.of(
                "groq", model(false, "key"))), factory))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void summariesListDefaultFirstWithoutSecrets() {
        ChatModelRegistry registry = new ChatModelRegistry(properties("openai", Map.of(
                "groq", model(true, "groq-secret"),
                "openai", model(true, "openai-secret"))), factory);

        assertThat(registry.summaries()).extracting(ChatModelRegistry.ModelSummary::name)
                .containsExactly("openai", "groq");
        assertThat(registry.summaries().getFirst().isDefault()).isTrue();
        assertThat(registry.summaries().toString()).doesNotContain("secret", "https://");
    }

    @Test
    void realFactoryBuildsOpenAiCompatibleModel() {
        ChatModelRegistry registry = new ChatModelRegistry(properties("groq", Map.of(
                "groq", model(true, "key"))));

        assertThat(registry.resolve(null).chatModel()).isInstanceOf(OpenAiChatModel.class);
    }

    private static AiModelProperties properties(String defaultModel, Map<String, AiModelProperties.Model> models) {
        return new AiModelProperties(defaultModel, Duration.ofSeconds(30), models);
    }

    private static AiModelProperties.Model model(boolean enabled, String apiKey) {
        return new AiModelProperties.Model(enabled, "https://api.example.com/v1", apiKey, null, "some-model", null);
    }
}
