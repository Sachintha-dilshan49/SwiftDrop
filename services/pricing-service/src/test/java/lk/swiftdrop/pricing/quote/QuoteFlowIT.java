package lk.swiftdrop.pricing.quote;

import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import lk.swiftdrop.pricing.support.AbstractPostgresIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import static lk.swiftdrop.pricing.support.QuoteSamples.WORKED_EXAMPLE_JSON;
import static lk.swiftdrop.pricing.support.QuoteSamples.WORKED_EXAMPLE_QUERY;
import static org.assertj.core.api.Assertions.assertThat;

/** The quote end to end over HTTP: security, validation, the seeded rules in PostgreSQL and the calculation. */
class QuoteFlowIT extends AbstractPostgresIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {
                    // assertions check the status
                })
                .build();
    }

    @AfterEach
    void removeTestPromoCodes() {
        jdbc.update("DELETE FROM promo_codes WHERE code LIKE 'IT_%'");
    }

    @Test
    @DisplayName("parcel-service's quote for the worked example with DROP100 is 390.00 from the seeded rules")
    void internalQuoteForTheWorkedExampleIs390() {
        ResponseEntity<String> response = client.post().uri("/internal/pricing/quote")
                .header(InternalTokenAuthenticationFilter.HEADER, InternalTokenTestContextCustomizerFactory.TEST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(WORKED_EXAMPLE_JSON)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualToIgnoringWhitespace("""
                {"routeType":"NEIGHBOURING","baseFee":350.00,"weightFee":40.00,"sizeFee":0.00,
                 "expressFee":100.00,"discount":100.00,"total":390.00,"currency":"LKR"}
                """);
    }

    @Test
    void guestEstimateWithoutATokenMatchesTheQuote() {
        ResponseEntity<String> response = client.get().uri("/api/v1/pricing/estimate?" + WORKED_EXAMPLE_QUERY)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"total\":390.00", "\"routeType\":\"NEIGHBOURING\"");
    }

    @Test
    void quoteWithoutTheInternalTokenIsUnauthorized() {
        ResponseEntity<String> response = client.post().uri("/internal/pricing/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .body(WORKED_EXAMPLE_JSON)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredPromoCodeIsA400WithAClearDetail() {
        jdbc.update("""
                INSERT INTO promo_codes (code, discount_amount, valid_from, valid_to)
                VALUES ('IT_EXPIRED', 50, now() - interval '2 days', now() - interval '1 day')
                """);

        ResponseEntity<String> response = client.get()
                .uri("/api/v1/pricing/estimate?" + WORKED_EXAMPLE_QUERY.replace("DROP100", "it_expired"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).contains("\"detail\":\"Promo code IT_EXPIRED has expired.\"");
    }

    @Test
    void parcelTooHeavyForItsSizeIsA400() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/pricing/estimate?" + WORKED_EXAMPLE_QUERY.replace("weightKg=1.2", "weightKg=7"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("A SMALL parcel can weigh at most 5 kg. Choose a larger size.");
    }

    @Test
    @DisplayName("The public zone list has the nine seeded zones with their districts")
    void zoneListHasTheNineSeededZones() {
        ResponseEntity<String> response = client.get().uri("/api/v1/pricing/zones")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("COLOMBO_CENTRAL", "COLOMBO_SOUTH", "COLOMBO_NORTH",
                "KOTTE_RAJAGIRIYA", "NUGEGODA_MAHARAGAMA", "DEHIWALA_MT_LAVINIA", "KANDY", "GALLE", "NEGOMBO",
                "\"districts\":[\"Colombo 04\",\"Colombo 05\",\"Colombo 06\"]");
    }
}
