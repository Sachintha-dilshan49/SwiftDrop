package lk.swiftdrop.ai.tools;

/**
 * Keys of the Spring AI {@code ToolContext} map. The context is passed to every tool call and is
 * never sent to the model.
 */
public final class ToolContextKeys {

    /** Caller's JWT (without {@code Bearer }), set by the chat service. */
    public static final String BEARER_TOKEN = "swiftdrop.bearerToken";

    /** {@link ToolCallTracker} for the current chat request, set by the chat service. */
    public static final String TRACKER = "swiftdrop.toolCallTracker";

    /** Set by Spring AI's MCP server: the MCP transport context of the current request. */
    public static final String MCP_EXCHANGE = "exchange";

    /** Key in the MCP transport context that holds the request's {@code Authorization} header. */
    public static final String MCP_AUTHORIZATION = "authorization";

    private ToolContextKeys() {
    }
}
