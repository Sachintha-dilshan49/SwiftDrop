package lk.swiftdrop.parcel.parcel.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import lk.swiftdrop.parcel.parcel.dto.CreateInternalParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.CreateInternalParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.RecipientContactsResponse;
import lk.swiftdrop.parcel.parcel.dto.UpdateParcelStatusRequest;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Only callers with the shared internal token reach these endpoints (ADR-010); a user JWT alone,
 * whatever its role, gets 401 from {@code common} before the controller runs.
 */
@WebMvcTest(controllers = InternalParcelController.class)
class InternalParcelControllerTest {

    private static final String TOKEN_HEADER = InternalTokenAuthenticationFilter.HEADER;
    private static final String TOKEN = InternalTokenTestContextCustomizerFactory.TEST_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ParcelService parcelService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID parcelId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setupTokens() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            String role = switch (token) {
                case "customer-token" -> "CUSTOMER";
                case "merchant-token" -> "MERCHANT";
                case "courier-token" -> "COURIER";
                case "dispatcher-token" -> "DISPATCHER";
                case "admin-token" -> "ADMIN";
                default -> throw new BadJwtException("Invalid token");
            };
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .subject(callerId.toString())
                    .claim("roles", List.of(role))
                    .build();
        });
    }

    @Test
    @DisplayName("POST /internal/parcels with the internal token returns 201 with only the order fields")
    void createWithInternalTokenReturnsCreated() throws Exception {
        given(parcelService.createMerchantParcel(any(CreateInternalParcelRequest.class)))
                .willReturn(new CreateInternalParcelResponse(parcelId, "SD-9B2N-4X1Z", ParcelStatus.CREATED,
                        new BigDecimal("730.00"), "LKR"));

        mockMvc.perform(post("/internal/parcels")
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/internal/parcels/" + parcelId))
                .andExpect(jsonPath("$.id").value(parcelId.toString()))
                .andExpect(jsonPath("$.trackingNo").value("SD-9B2N-4X1Z"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.feeAmount").value(730.00))
                .andExpect(jsonPath("$.feeCurrency").value("LKR"))
                .andExpect(jsonPath("$.recipientPhone").doesNotExist());

        verify(parcelService).createMerchantParcel(createRequest());
    }

    @Test
    @DisplayName("A fee sent by the merchant is ignored, so the merchant cannot set its own price")
    void merchantFeeIsIgnored() throws Exception {
        given(parcelService.createMerchantParcel(any(CreateInternalParcelRequest.class)))
                .willReturn(new CreateInternalParcelResponse(parcelId, "SD-9B2N-4X1Z", ParcelStatus.CREATED,
                        new BigDecimal("730.00"), "LKR"));
        Map<String, Object> body = objectMapper.convertValue(createRequest(), new TypeReference<>() {
        });
        body.put("feeAmount", 1.00);

        mockMvc.perform(post("/internal/parcels")
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeAmount").value(730.00));

        verify(parcelService).createMerchantParcel(createRequest());
    }

    @Test
    @DisplayName("POST /internal/parcels with missing or invalid fields returns 400 with field errors")
    void createWithInvalidFieldsReturnsBadRequest() throws Exception {
        Map<String, Object> body = objectMapper.convertValue(createRequest(), new TypeReference<>() {
        });
        body.remove("pickupZone");
        body.remove("speed");
        body.put("recipientEmail", "not-an-email");

        mockMvc.perform(post("/internal/parcels")
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("pickupZone", "speed", "recipientEmail")));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("POST /internal/parcels without a token returns 401")
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("POST /internal/parcels with a wrong internal token returns 401")
    void createWithWrongTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/parcels")
                        .header(TOKEN_HEADER, "not-the-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(parcelService);
    }

    @ParameterizedTest(name = "POST create with only a user JWT ({0}) returns 401")
    @ValueSource(strings = {"customer-token", "merchant-token", "courier-token", "dispatcher-token", "admin-token"})
    void createWithUserJwtOnlyIsUnauthorized(String token) throws Exception {
        mockMvc.perform(post("/internal/parcels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("PATCH /internal/parcels/{id}/status with the internal token passes the caller from the body to the service")
    void statusUpdateWithInternalTokenReachesService() throws Exception {
        given(parcelService.updateParcelStatus(eq(parcelId), any(UpdateParcelStatusRequest.class)))
                .willReturn(parcelResponse(ParcelStatus.PICKED_UP));

        mockMvc.perform(patch("/internal/parcels/" + parcelId + "/status")
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest("COURIER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        // dispatch-service vouches for the caller; ParcelStateMachine checks the role and the assignment
        ArgumentCaptor<UpdateParcelStatusRequest> passed = ArgumentCaptor.forClass(UpdateParcelStatusRequest.class);
        verify(parcelService).updateParcelStatus(eq(parcelId), passed.capture());
        assertThat(passed.getValue().changedBy()).isEqualTo(callerId);
        assertThat(passed.getValue().changedByRole()).isEqualTo("COURIER");
    }

    @Test
    @DisplayName("PATCH /internal/parcels/{id}/status without a token returns 401")
    void statusUpdateWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/internal/parcels/" + parcelId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest("COURIER"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));

        verifyNoInteractions(parcelService);
    }

    @ParameterizedTest(name = "PATCH status with only a user JWT ({0}) returns 401")
    @ValueSource(strings = {"customer-token", "merchant-token", "courier-token", "dispatcher-token", "admin-token"})
    void statusUpdateWithUserJwtOnlyIsUnauthorized(String token) throws Exception {
        mockMvc.perform(patch("/internal/parcels/" + parcelId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest("ADMIN"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("PATCH /internal/parcels/{id}/status without a status returns 400 with a field error")
    void statusUpdateWithoutStatusIsBadRequest() throws Exception {
        mockMvc.perform(patch("/internal/parcels/" + parcelId + "/status")
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changedBy\":\"" + callerId + "\",\"changedByRole\":\"DISPATCHER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("status")));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("A transition the state machine rejects comes back as a 409 ProblemDetail")
    void rejectedTransitionReturnsConflict() throws Exception {
        given(parcelService.updateParcelStatus(eq(parcelId), any(UpdateParcelStatusRequest.class)))
                .willThrow(new ConflictException("invalid-status-transition", "Invalid status transition",
                        "Parcel cannot move from DELIVERED to IN_TRANSIT."));

        mockMvc.perform(patch("/internal/parcels/" + parcelId + "/status")
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest("DISPATCHER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/invalid-status-transition"));
    }

    @Test
    @DisplayName("GET /internal/parcels/{id}/contacts with the internal token returns the recipient's contacts")
    void contactsWithInternalTokenReturnRecipient() throws Exception {
        given(parcelService.getRecipientContacts(parcelId))
                .willReturn(new RecipientContactsResponse("Sunil Fernando", "+94771234567", "sunil@example.com"));

        mockMvc.perform(get("/internal/parcels/" + parcelId + "/contacts").header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Sunil Fernando"))
                .andExpect(jsonPath("$.recipientPhone").value("+94771234567"))
                .andExpect(jsonPath("$.recipientEmail").value("sunil@example.com"));
    }

    @Test
    @DisplayName("GET contacts of a customer booking returns recipientEmail as null")
    void contactsOfCustomerBookingHaveNullEmail() throws Exception {
        given(parcelService.getRecipientContacts(parcelId))
                .willReturn(new RecipientContactsResponse("Nimal Perera", "+94771234567", null));

        mockMvc.perform(get("/internal/parcels/" + parcelId + "/contacts").header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Nimal Perera"))
                .andExpect(jsonPath("$.recipientEmail").value(nullValue()));
    }

    @Test
    @DisplayName("GET contacts of an unknown parcel returns 404")
    void contactsOfUnknownParcelAreNotFound() throws Exception {
        given(parcelService.getRecipientContacts(parcelId))
                .willThrow(new NotFoundException("Parcel not found: " + parcelId));

        mockMvc.perform(get("/internal/parcels/" + parcelId + "/contacts").header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/not-found"));
    }

    @Test
    @DisplayName("GET contacts without a token returns 401")
    void contactsWithoutTokenAreUnauthorized() throws Exception {
        mockMvc.perform(get("/internal/parcels/" + parcelId + "/contacts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));

        verifyNoInteractions(parcelService);
    }

    @ParameterizedTest(name = "GET contacts with only a user JWT ({0}) returns 401")
    @ValueSource(strings = {"customer-token", "merchant-token", "courier-token", "dispatcher-token", "admin-token"})
    void contactsWithUserJwtOnlyAreUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/internal/parcels/" + parcelId + "/contacts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(parcelService);
    }

    private UpdateParcelStatusRequest statusRequest(String claimedRole) {
        return new UpdateParcelStatusRequest(ParcelStatus.PICKED_UP, callerId, "Picked up", null, null,
                callerId, claimedRole);
    }

    private ParcelResponse parcelResponse(ParcelStatus status) {
        return new ParcelResponse(parcelId, "SD-9B2N-4X1Z", null, UUID.randomUUID(), callerId, "Pickup", "Dropoff",
                "Recipient", "+94771234567", ParcelSize.SMALL, new BigDecimal("1.50"), new BigDecimal("350.00"),
                "LKR", status, null, Instant.now(), null, List.of());
    }

    private static CreateInternalParcelRequest createRequest() {
        return new CreateInternalParcelRequest(UUID.fromString("5e1c0000-4444-4c2e-9a71-0d6f2b9c1e44"),
                "Warehouse Colombo", "COLOMBO_SOUTH", "Customer Galle", "INTER_CITY", "Sunil Fernando",
                "+94771234567", "sunil@example.com", ParcelSize.SMALL, new BigDecimal("1.50"), DeliverySpeed.STANDARD);
    }
}
