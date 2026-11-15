package lk.swiftdrop.ai.tools;

import io.modelcontextprotocol.common.McpTransportContext;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Who is calling a tool, read from the {@link ToolContext}.
 *
 * <ul>
 *   <li>Chat: the chat service puts the caller's JWT and a {@link ToolCallTracker} in the context.</li>
 *   <li>MCP: Spring AI puts the MCP transport context under {@code exchange}; our transport copies
 *       the request's {@code Authorization} header into it (see {@code McpServerConfig}). There is no
 *       tracker, because MCP clients run their own model and decide how many calls to make.</li>
 * </ul>
 */
public final class ToolCaller {

    static final String NOT_SIGNED_IN = "The user is not signed in, so this information is not available.";
    private static final String BEARER_PREFIX = "Bearer ";

    private ToolCaller() {
    }

    /**
     * Records the call (enforcing the per-request limit in chat) and returns the caller's JWT.
     *
     * @return the JWT without {@code Bearer }, or {@code null} if the request carried none
     * @throws ToolFailureException if the limit is reached
     */
    public static String begin(ToolContext toolContext, String toolName) {
        Map<String, Object> context = toolContext == null ? Map.of() : toolContext.getContext();
        if (context.get(ToolContextKeys.TRACKER) instanceof ToolCallTracker tracker) {
            tracker.record(toolName);
        }
        return bearerToken(context);
    }

    /**
     * Same as {@link #begin} for tools that act on the caller's own data.
     *
     * @throws ToolFailureException if the limit is reached or there is no token
     */
    public static String beginSignedIn(ToolContext toolContext, String toolName) {
        String token = begin(toolContext, toolName);
        if (token == null) {
            throw new ToolFailureException(NOT_SIGNED_IN);
        }
        return token;
    }

    static String bearerToken(Map<String, Object> context) {
        if (context.get(ToolContextKeys.BEARER_TOKEN) instanceof String token && !token.isBlank()) {
            return token;
        }
        if (context.get(ToolContextKeys.MCP_EXCHANGE) instanceof McpTransportContext transport
                && transport.get(ToolContextKeys.MCP_AUTHORIZATION) instanceof String header
                && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
