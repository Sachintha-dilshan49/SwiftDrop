package lk.swiftdrop.common.error;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import lk.swiftdrop.common.web.RequestIds;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Builds and completes RFC 7807 bodies in the shape defined in docs/conventions.md. */
public final class ProblemDetails {

    public static final String TYPE_BASE = "https://swiftdrop.lk/problems/";
    public static final String VALIDATION_ERROR = "validation-error";
    public static final String VALIDATION_TITLE = "Validation failed";
    public static final String REQUEST_ID = "requestId";
    public static final String ERRORS = "errors";

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatusCode status, String problemType, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type(problemType));
        problem.setTitle(title);
        return problem;
    }

    public static URI type(String problemType) {
        return URI.create(TYPE_BASE + problemType);
    }

    public static void setErrors(ProblemDetail problem, List<FieldErrorDetail> errors) {
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS, errors);
        }
    }

    /**
     * Fills the parts every SwiftDrop problem has: a SwiftDrop {@code type} instead of
     * {@code about:blank}, the request path as {@code instance}, and the current request ID.
     */
    public static ProblemDetail complete(ProblemDetail problem, String requestPath) {
        if (problem.getType() == null || ABOUT_BLANK.equals(problem.getType())) {
            problem.setType(type(slug(problem.getStatus())));
        }
        if (problem.getInstance() == null && requestPath != null) {
            problem.setInstance(URI.create(requestPath));
        }
        String requestId = RequestIds.current();
        if (requestId != null) {
            problem.setProperty(REQUEST_ID, requestId);
        }
        return problem;
    }

    /** {@code 405} becomes {@code method-not-allowed}. */
    static String slug(int status) {
        HttpStatus known = HttpStatus.resolve(status);
        return known == null ? "http-" + status : known.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
