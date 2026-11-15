package lk.swiftdrop.dispatch.courier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.internal.InternalRestClientFactory;
import lk.swiftdrop.dispatch.config.RestClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Runs the client against a fake auth-service over real HTTP. */
class AuthServiceClientTest {

    @RegisterExtension
    static WireMockExtension authService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PATH = "/internal/users/" + USER_ID + "/contact";

    @Test
    void readsTheFirstNameWithTheInternalToken() {
        authService.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"email":"kasun@example.com","phone":"0771234567","firstName":"Kasun"}
                """)));

        assertThat(client().firstName(USER_ID)).contains("Kasun");
        authService.verify(getRequestedFor(urlPathEqualTo(PATH))
                .withHeader("X-Internal-Token", equalTo("test-internal-token")));
    }

    @Test
    void unknownUserHasNoName() {
        authService.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

        assertThat(client().firstName(USER_ID)).isEmpty();
    }

    @Test
    void blankNameCountsAsNoName() {
        authService.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"email":"kasun@example.com","firstName":" "}
                """)));

        assertThat(client().firstName(USER_ID)).isEmpty();
    }

    @Test
    void serverErrorBecomesServiceUnavailable() {
        authService.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client().firstName(USER_ID)).isInstanceOf(ServiceUnavailableException.class);
    }

    private static AuthServiceClient client() {
        return new AuthServiceClient(new RestClientConfig().authServiceRestClient(
                new InternalRestClientFactory("test-internal-token"), authService.baseUrl()));
    }
}
