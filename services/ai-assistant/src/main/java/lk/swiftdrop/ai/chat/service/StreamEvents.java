package lk.swiftdrop.ai.chat.service;

import java.util.List;
import java.util.UUID;
import lk.swiftdrop.ai.chat.dto.SourceRef;

/** Server-sent event names and payloads of {@code POST /api/v1/assistant/chat/stream}. */
public final class StreamEvents {

    public static final String TOKEN = "token";
    public static final String DONE = "done";
    public static final String ERROR = "error";

    private StreamEvents() {
    }

    /** A piece of the reply. JSON, so leading spaces survive the SSE format. */
    public record Token(String text) {
    }

    /** Sent once after the last token. */
    public record Done(UUID conversationId, String model, List<String> toolsUsed, List<SourceRef> sources) {
    }

    /** The reply failed after streaming had started. */
    public record Error(String detail) {
    }
}
