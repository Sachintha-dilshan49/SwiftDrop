package lk.swiftdrop.ai.model;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

/** 503: the chosen model has no API key, or the provider failed or timed out. */
public class AssistantUnavailableException extends SwiftDropException {

    public AssistantUnavailableException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "assistant-unavailable", "Assistant unavailable", detail);
    }
}
