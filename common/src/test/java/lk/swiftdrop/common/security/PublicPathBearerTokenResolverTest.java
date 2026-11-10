package lk.swiftdrop.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

class PublicPathBearerTokenResolverTest {

    private final BearerTokenResolver resolver =
            new PublicPathBearerTokenResolver(PathPatternRequestMatcher.withDefaults().matcher("/public/**"));

    @Test
    void returnsNoTokenOnPublicPath() {
        assertThat(resolver.resolve(request("/public/thing", "Bearer abc"))).isNull();
    }

    @Test
    void readsBearerTokenOnOtherPaths() {
        assertThat(resolver.resolve(request("/private/thing", "Bearer abc"))).isEqualTo("abc");
    }

    @Test
    void delegatesToGivenResolverOnOtherPaths() {
        BearerTokenResolver custom = new PublicPathBearerTokenResolver(
                PathPatternRequestMatcher.withDefaults().matcher("/public/**"), request -> "from-delegate");

        assertThat(custom.resolve(request("/private/thing", null))).isEqualTo("from-delegate");
        assertThat(custom.resolve(request("/public/thing", null))).isNull();
    }

    private static MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return request;
    }
}
