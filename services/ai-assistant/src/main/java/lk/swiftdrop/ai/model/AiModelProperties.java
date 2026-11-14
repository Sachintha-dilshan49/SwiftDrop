package lk.swiftdrop.ai.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code swiftdrop.ai.*}: the chat models the assistant can use. Every entry is an
 * OpenAI-compatible API (Groq, OpenAI, OpenRouter, a local Ollama, ...).
 *
 * @param defaultModel name of the entry used when a request names no model
 * @param timeout      timeout of one call to the provider
 * @param models       configured models by name; names are what clients may send
 */
@Validated
@ConfigurationProperties("swiftdrop.ai")
public record AiModelProperties(
        @NotBlank String defaultModel,
        @NotNull Duration timeout,
        @NotNull Map<@Pattern(regexp = ModelNames.PATTERN) String, @Valid @NotNull Model> models) {

    public AiModelProperties {
        models = models == null ? Map.of() : Map.copyOf(models);
    }

    /**
     * @param enabled     whether clients may use this model
     * @param baseUrl     API base URL including the version path, e.g. {@code https://api.groq.com/openai/v1}
     * @param apiKey      secret, from an environment variable only; blank makes the model unavailable
     *                    unless {@code requiresKey} is false
     * @param requiresKey false for local servers such as Ollama
     * @param model       the provider's model id
     * @param temperature sampling temperature; low keeps answers and tool choice consistent
     */
    public record Model(
            boolean enabled,
            @NotBlank String baseUrl,
            String apiKey,
            Boolean requiresKey,
            @NotBlank String model,
            @DecimalMin("0.0") @DecimalMax("2.0") Double temperature) {

        public Model {
            requiresKey = requiresKey == null || requiresKey;
            temperature = temperature == null ? 0.2 : temperature;
        }

        boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
