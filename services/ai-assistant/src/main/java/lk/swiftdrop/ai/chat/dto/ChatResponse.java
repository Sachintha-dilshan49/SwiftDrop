package lk.swiftdrop.ai.chat.dto;

import java.util.List;
import java.util.UUID;

/**
 * @param conversationId send this back to continue the conversation
 * @param model          name of the model that answered
 * @param reply          the assistant's answer
 * @param toolsUsed      tools called for this answer, e.g. {@code ["track_parcel"]}
 * @param sources        help articles the answer was based on
 */
public record ChatResponse(
        UUID conversationId,
        String model,
        String reply,
        List<String> toolsUsed,
        List<SourceRef> sources) {
}
