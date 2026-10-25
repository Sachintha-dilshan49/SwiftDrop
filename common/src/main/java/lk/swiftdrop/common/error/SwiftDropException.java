package lk.swiftdrop.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain exceptions that the shared handler turns into an RFC 7807 response.
 *
 * <p>The {@code detail} message is returned to the client as-is, so it must never contain
 * personal data (names, emails, phone numbers, addresses) or tokens.
 */
public abstract class SwiftDropException extends RuntimeException {

    private final HttpStatus status;
    private final String problemType;
    private final String title;

    /**
     * @param status      HTTP status to return
     * @param problemType short kebab-case slug, appended to {@link ProblemDetails#TYPE_BASE}
     * @param title       short, human-readable summary of the problem type
     * @param detail      explanation of this occurrence; safe to show to the client
     */
    protected SwiftDropException(HttpStatus status, String problemType, String title, String detail) {
        super(detail);
        this.status = status;
        this.problemType = problemType;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getProblemType() {
        return problemType;
    }

    public String getTitle() {
        return title;
    }
}
