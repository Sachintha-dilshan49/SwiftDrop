package lk.swiftdrop.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lk.swiftdrop.ai.model.ModelNames;

/**
 * @param message        the user's question
 * @param conversationId conversation to continue, or {@code null} to start a new one
 * @param model          configured model name, or {@code null} for the default
 */
public record ChatRequest(
        @NotBlank @Size(max = 2000) String message,
        UUID conversationId,
        @Pattern(regexp = ModelNames.PATTERN, message = "must be a model name from GET /api/v1/assistant/models")
        String model) {
}
