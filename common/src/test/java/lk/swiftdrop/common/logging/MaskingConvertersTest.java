package lk.swiftdrop.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

class MaskingConvertersTest {

    private final LoggerContext context = new LoggerContext();

    @Test
    void messageConverterMasksFormattedArguments() {
        MaskingMessageConverter converter = new MaskingMessageConverter();
        converter.setContext(context);
        converter.start();

        LoggingEvent event = event("Notified {} at {}", null, "jane@example.com", "0771234567");

        assertThat(converter.convert(event)).isEqualTo("Notified [EMAIL] at [PHONE]");
    }

    @Test
    void throwableConverterMasksExceptionMessagesButKeepsStackTrace() {
        MaskingThrowableConverter converter = new MaskingThrowableConverter();
        converter.setContext(context);
        converter.start();

        LoggingEvent event = event("Lookup failed", new IllegalStateException("no user jane@example.com"));
        String output = converter.convert(event);

        assertThat(output)
                .contains("java.lang.IllegalStateException: no user [EMAIL]")
                .contains("at lk.swiftdrop.common.logging.MaskingConvertersTest")
                .doesNotContain("jane@example.com");
    }

    private LoggingEvent event(String message, Throwable throwable, Object... args) {
        return new LoggingEvent(MaskingConvertersTest.class.getName(), context.getLogger("test"), Level.INFO,
                message, throwable, args);
    }
}
