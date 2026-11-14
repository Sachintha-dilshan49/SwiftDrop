package lk.swiftdrop.ai.tools;

/**
 * A tool could not produce a result. Spring AI passes the message to the model as the tool
 * result, so the model can tell the user what went wrong. The message is shown to the model and
 * may be repeated to the user: it must never contain personal data, tokens or internal details.
 */
public class ToolFailureException extends RuntimeException {

    public ToolFailureException(String safeMessage) {
        super(safeMessage);
    }
}
