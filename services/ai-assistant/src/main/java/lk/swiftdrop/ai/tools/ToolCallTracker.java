package lk.swiftdrop.ai.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Counts the tool calls of one chat request and remembers which tools were used. It stops a model
 * that keeps calling tools in a loop: once the limit is reached, further calls fail with a message
 * that tells the model to answer with what it has.
 *
 * <p>One instance per request. Tools may run on another thread when streaming, so it is
 * synchronized.
 */
public class ToolCallTracker {

    public static final int DEFAULT_MAX_CALLS = 5;

    static final String LIMIT_REACHED =
            "Tool call limit reached for this message. Answer with the information you already have.";

    private final int maxCalls;
    private final List<String> calls = new ArrayList<>();

    public ToolCallTracker() {
        this(DEFAULT_MAX_CALLS);
    }

    public ToolCallTracker(int maxCalls) {
        this.maxCalls = maxCalls;
    }

    /**
     * Records a call to {@code toolName}.
     *
     * @throws ToolFailureException if the limit has been reached
     */
    public synchronized void record(String toolName) {
        if (calls.size() >= maxCalls) {
            throw new ToolFailureException(LIMIT_REACHED);
        }
        calls.add(toolName);
    }

    /** Distinct tool names in the order they were first used. */
    public synchronized List<String> toolsUsed() {
        return calls.stream().distinct().toList();
    }
}
