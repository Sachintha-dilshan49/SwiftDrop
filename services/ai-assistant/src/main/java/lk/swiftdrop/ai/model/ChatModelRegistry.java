package lk.swiftdrop.ai.model;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The configured chat models, by name. One OpenAI-compatible {@link ChatModel} is built per enabled
 * entry, so adding a provider that speaks the OpenAI API is configuration only.
 *
 * <p>Startup fails if the default model is not configured or not enabled. A model without an API
 * key is listed but unavailable, so the service (and {@code docker compose up}) still starts
 * without a key; chat then answers 503 until the key is set.
 */
@Component
public class ChatModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChatModelRegistry.class);

    static final String NO_KEY = "The AI model '%s' is not configured on this server yet. Try again later.";

    private final String defaultModel;
    private final Map<String, Entry> entries;

    @Autowired
    public ChatModelRegistry(AiModelProperties properties) {
        this(properties, ChatModelRegistry::openAiCompatible);
    }

    ChatModelRegistry(AiModelProperties properties, Function<ModelSettings, ChatModel> factory) {
        AiModelProperties.Model defaults = properties.models().get(properties.defaultModel());
        if (defaults == null || !defaults.enabled()) {
            throw new IllegalStateException("swiftdrop.ai.default-model '" + properties.defaultModel()
                    + "' must name an enabled entry of swiftdrop.ai.models");
        }
        this.defaultModel = properties.defaultModel();

        Map<String, Entry> built = new LinkedHashMap<>();
        properties.models().forEach((name, model) -> {
            if (!model.enabled()) {
                return;
            }
            ChatModel chatModel = null;
            if (model.hasKey() || !model.requiresKey()) {
                chatModel = factory.apply(new ModelSettings(model, properties.timeout()));
            } else {
                log.warn("AI model '{}' has no API key; it stays unavailable until one is set", name);
            }
            built.put(name, new Entry(name, model.model(), chatModel));
        });
        this.entries = Map.copyOf(built);
        log.info("AI models enabled: {} (default '{}')", built.keySet(), defaultModel);
    }

    /**
     * The model to use for a request.
     *
     * @param name requested model name, or {@code null} for the default
     * @throws UnknownModelException          if the name is not an enabled model (400)
     * @throws AssistantUnavailableException if the model has no API key (503)
     */
    public ResolvedModel resolve(String name) {
        String key = name == null || name.isBlank() ? defaultModel : name;
        Entry entry = entries.get(key);
        if (entry == null) {
            throw new UnknownModelException(key);
        }
        if (entry.chatModel() == null) {
            throw new AssistantUnavailableException(NO_KEY.formatted(key));
        }
        return new ResolvedModel(entry.name(), entry.chatModel());
    }

    /** Enabled models for a picker, default first. Never includes URLs or keys. */
    public List<ModelSummary> summaries() {
        return entries.values().stream()
                .sorted(Comparator.comparing((Entry e) -> !e.name().equals(defaultModel)).thenComparing(Entry::name))
                .map(e -> new ModelSummary(e.name(), e.providerModel(), e.name().equals(defaultModel),
                        e.chatModel() != null))
                .toList();
    }

    public String defaultModel() {
        return defaultModel;
    }

    static ChatModel openAiCompatible(ModelSettings settings) {
        AiModelProperties.Model model = settings.model();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(model.baseUrl())
                .apiKey(model.hasKey() ? model.apiKey() : "not-needed")
                .model(model.model())
                .temperature(model.temperature())
                .timeout(settings.timeout())
                .maxRetries(1)
                // Groq rejects reasoning_content in earlier messages, which breaks every tool call
                .replayReasoningContent(false)
                .build();
        return OpenAiChatModel.builder().options(options).build();
    }

    /** Input to the model factory: one configured model plus the shared timeout. */
    record ModelSettings(AiModelProperties.Model model, Duration timeout) {
    }

    /** A model ready to use. */
    public record ResolvedModel(String name, ChatModel chatModel) {
    }

    /** What {@code GET /api/v1/assistant/models} shows for one model. */
    public record ModelSummary(String name, String model, boolean isDefault, boolean available) {
    }

    private record Entry(String name, String providerModel, ChatModel chatModel) {
    }
}
