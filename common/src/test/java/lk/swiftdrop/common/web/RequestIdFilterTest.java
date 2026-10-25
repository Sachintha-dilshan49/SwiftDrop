package lk.swiftdrop.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void keepsValidIncomingRequestIdAndExposesItInMdcDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIds.HEADER, "gw-7f3a.1_b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInChain.set(RequestIds.current());
            }
        });

        assertThat(seenInChain.get()).isEqualTo("gw-7f3a.1_b");
        assertThat(response.getHeader(RequestIds.HEADER)).isEqualTo("gw-7f3a.1_b");
        assertThat(MDC.get(RequestIds.MDC_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader(RequestIds.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    void replacesUnsafeOrOversizedRequestIds() {
        assertThat(RequestIds.sanitizeOrGenerate("abc\r\nINFO forged log line")).matches("[0-9a-f-]{36}");
        assertThat(RequestIds.sanitizeOrGenerate("x".repeat(65))).matches("[0-9a-f-]{36}");
        assertThat(RequestIds.sanitizeOrGenerate("")).matches("[0-9a-f-]{36}");
    }

    @Test
    void clearsMdcEvenWhenTheChainThrows() {
        MockFilterChain failing = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new IllegalStateException("boom");
            }
        };

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), failing);
        } catch (Exception expected) {
            // the filter must not swallow it
        }

        assertThat(MDC.get(RequestIds.MDC_KEY)).isNull();
    }
}
