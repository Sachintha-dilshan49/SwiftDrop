package lk.swiftdrop.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * A 4xx that the shared internal client does not map (e.g. 401 from a wrong X-Internal-Token) must come back as
 * "no data". If it escaped, the event would be redelivered and the notifications already sent sent again.
 */
class ClientFailureTest {

    private static final UUID ID = UUID.randomUUID();

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void parcelContactsLookupGivesNothingOnAClientError(int status) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://parcel");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://parcel/internal/parcels/" + ID + "/contacts"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThat(new ParcelServiceClient(builder.build()).getRecipientContacts(ID)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void userContactLookupGivesNothingOnAClientError(int status) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/internal/users/" + ID + "/contact"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThat(new AuthServiceClient(builder.build()).getUserContact(ID)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void webhookConfigLookupGivesNothingOnAClientError(int status) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://merchant");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://merchant/internal/merchants/" + ID + "/webhook"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThat(new MerchantServiceClient(builder.build()).getWebhookConfig(ID)).isEmpty();
    }
}
