package lk.swiftdrop.parcel.parcel.controller;

import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.dto.PublicStatusHistoryResponse;
import lk.swiftdrop.parcel.parcel.dto.PublicTrackingResponse;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uses the real {@code swiftdrop.security.public-paths} from application.yml. */
@WebMvcTest(controllers = PublicTrackingController.class)
class PublicTrackingControllerTest {

    private static final String TRACKING_NO = "SD-7K3Q-9P2M";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParcelService parcelService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final PublicTrackingResponse tracking = new PublicTrackingResponse(
            TRACKING_NO,
            ParcelStatus.IN_TRANSIT,
            ParcelSize.MEDIUM,
            new BigDecimal("2.50"),
            Instant.parse("2026-11-06T09:00:00Z"),
            List.of(
                    new PublicStatusHistoryResponse(ParcelStatus.CREATED, Instant.parse("2026-11-06T09:00:00Z")),
                    new PublicStatusHistoryResponse(ParcelStatus.PICKED_UP, Instant.parse("2026-11-06T10:30:00Z")),
                    new PublicStatusHistoryResponse(ParcelStatus.IN_TRANSIT, Instant.parse("2026-11-06T12:00:00Z"))
            )
    );

    @BeforeEach
    void setupTokens() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if (token.equals("customer-token")) {
                return Jwt.withTokenValue(token).header("alg", "RS256")
                        .subject(UUID.randomUUID().toString()).claim("roles", List.of("CUSTOMER")).build();
            }
            throw new BadJwtException("Invalid token");
        });
    }

    @Test
    @DisplayName("GET /api/v1/track/{trackingNo} works without a token and returns only public fields")
    void trackingIsPublicAndHasNoPrivateData() throws Exception {
        when(parcelService.getPublicTracking(TRACKING_NO)).thenReturn(tracking);

        mockMvc.perform(get("/api/v1/track/" + TRACKING_NO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNo").value(TRACKING_NO))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.size").value("MEDIUM"))
                .andExpect(jsonPath("$.weightKg").value(2.50))
                .andExpect(jsonPath("$.createdAt").value("2026-11-06T09:00:00Z"))
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[1].status").value("PICKED_UP"))
                .andExpect(jsonPath("$.history[1].occurredAt").value("2026-11-06T10:30:00Z"))
                .andExpect(jsonPath("$.history[1].note").doesNotExist())
                .andExpect(jsonPath("$.history[1].changedBy").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.merchantId").doesNotExist())
                .andExpect(jsonPath("$.courierId").doesNotExist())
                .andExpect(jsonPath("$.recipientName").doesNotExist())
                .andExpect(jsonPath("$.recipientPhone").doesNotExist())
                .andExpect(jsonPath("$.pickupAddress").doesNotExist())
                .andExpect(jsonPath("$.dropoffAddress").doesNotExist())
                .andExpect(jsonPath("$.failureReason").doesNotExist());
    }

    @Test
    @DisplayName("A signed-in customer can also use the public tracking page")
    void trackingWorksWithAValidToken() throws Exception {
        when(parcelService.getPublicTracking(TRACKING_NO)).thenReturn(tracking);

        mockMvc.perform(get("/api/v1/track/" + TRACKING_NO).header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tracking numbers are trimmed and upper-cased before the lookup")
    void trackingNumberIsNormalized() throws Exception {
        when(parcelService.getPublicTracking(TRACKING_NO)).thenReturn(tracking);

        mockMvc.perform(get("/api/v1/track/ sd-7k3q-9p2m "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNo").value(TRACKING_NO));
    }

    @Test
    @DisplayName("A well-formed but unknown tracking number returns 404 ProblemDetail")
    void unknownTrackingNumberIsNotFound() throws Exception {
        when(parcelService.getPublicTracking("SD-2222-2222"))
                .thenThrow(new NotFoundException("Tracking number not found: SD-2222-2222"));

        mockMvc.perform(get("/api/v1/track/SD-2222-2222"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/not-found"));
    }

    @Test
    @DisplayName("A malformed tracking number returns 400 ProblemDetail without a lookup")
    void malformedTrackingNumberIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/track/SD-UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("trackingNo"));

        verifyNoInteractions(parcelService);
    }
}
