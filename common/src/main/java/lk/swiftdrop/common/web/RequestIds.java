package lk.swiftdrop.common.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/** Request ID header and MDC key, shared by the filter, the error handler and the log pattern. */
public final class RequestIds {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // Incoming IDs are echoed into logs and responses, so only accept a short, safe charset.
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private RequestIds() {
    }

    /** The current request's ID, or {@code null} outside a request. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** Returns {@code candidate} if it is a safe ID, otherwise a new random one. */
    public static String sanitizeOrGenerate(String candidate) {
        if (candidate != null && VALID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
