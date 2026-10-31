package lk.swiftdrop.parcel.parcel.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.dto.CreateParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.PageResponse;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.ParcelSummaryResponse;
import lk.swiftdrop.parcel.parcel.dto.StatusHistoryResponse;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerParcelController.class)
class CustomerParcelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ParcelService parcelService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID customerId = UUID.fromString("6a1f0f7e-1111-4c2e-9a71-0d6f2b9c1e44");
    private final UUID parcelId = UUID.fromString("c41d0000-2222-4c2e-9a71-0d6f2b9c1e44");

    @BeforeEach
    void setupTokens() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            return switch (token) {
                case "customer-token" -> buildJwt(token, customerId, "CUSTOMER");
                case "courier-token" -> buildJwt(token, UUID.randomUUID(), "COURIER");
                case "merchant-token" -> buildJwt(token, UUID.randomUUID(), "MERCHANT");
                case "admin-token" -> buildJwt(token, adminId, "ADMIN");
                case "dispatcher-token" -> buildJwt(token, UUID.randomUUID(), "DISPATCHER");
                case "non-uuid-subject-token" -> buildJwt(token, "service-account", "CUSTOMER");
                default -> throw new BadJwtException("Invalid token");
            };
        });
    }

    private Jwt buildJwt(String token, UUID subject, String... roles) {
        return buildJwt(token, subject.toString(), roles);
    }

    private Jwt buildJwt(String token, String subject, String... roles) {
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(subject)
                .claim("roles", List.of(roles))
                .build();
    }

    private final UUID adminId = UUID.fromString("ad000000-3333-4c2e-9a71-0d6f2b9c1e44");

    private static CreateParcelRequest validRequest(String recipientPhone, BigDecimal weightKg) {
        return new CreateParcelRequest(
                "123 Main St, Colombo",
                "COLOMBO_SOUTH",
                "456 High St, Kandy",
                "INTER_CITY",
                "Nimal Perera",
                recipientPhone,
                ParcelSize.MEDIUM,
                weightKg,
                DeliverySpeed.EXPRESS,
                "DROP100"
        );
    }

    private static CreateParcelRequest validRequest() {
        return validRequest("+94771234567", new BigDecimal("2.50"));
    }

    /** A valid booking as JSON with one field changed. */
    private String validJsonWith(String field, Object value) throws Exception {
        Map<String, Object> body = objectMapper.convertValue(validRequest(), new TypeReference<>() {
        });
        body.put(field, value);
        return objectMapper.writeValueAsString(body);
    }

    private ParcelResponse createdResponse() {
        return new ParcelResponse(parcelId, "SD-7K3Q-9P2M", customerId, null, null, "Pickup", "Dropoff",
                "Recipient", "+94771234567", ParcelSize.MEDIUM, new BigDecimal("2.50"), new BigDecimal("650.00"),
                "LKR", ParcelStatus.CREATED, null, Instant.now(), null, List.of());
    }

    private static ParcelSummaryResponse summary(String trackingNo) {
        return new ParcelSummaryResponse(UUID.randomUUID(), trackingNo, ParcelStatus.CREATED, "Pickup", "Dropoff",
                "Recipient", "+94771234567", ParcelSize.SMALL, BigDecimal.ONE, new BigDecimal("350.00"), "LKR",
                Instant.now(), null);
    }

    @Test
    @DisplayName("POST /api/v1/parcels with valid request and CUSTOMER token returns 201 Created and Location header")
    void createParcelSuccess() throws Exception {
        CreateParcelRequest request = validRequest();

        ParcelResponse response = new ParcelResponse(
                parcelId,
                "SD-7K3Q-9P2M",
                customerId,
                null,
                null,
                request.pickupAddress(),
                request.dropoffAddress(),
                request.recipientName(),
                request.recipientPhone(),
                request.size(),
                request.weightKg(),
                new BigDecimal("650.00"),
                "LKR",
                ParcelStatus.CREATED,
                null,
                Instant.now(),
                null,
                List.of(new StatusHistoryResponse(ParcelStatus.CREATED, customerId, "CUSTOMER", "Created", null, Instant.now()))
        );

        when(parcelService.createCustomerParcel(eq(customerId), any(CreateParcelRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/parcels/" + parcelId))
                .andExpect(jsonPath("$.trackingNo").value("SD-7K3Q-9P2M"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.feeAmount").value(650.00));

        ArgumentCaptor<CreateParcelRequest> passed = ArgumentCaptor.forClass(CreateParcelRequest.class);
        verify(parcelService).createCustomerParcel(eq(customerId), passed.capture());
        assertThat(passed.getValue()).isEqualTo(request);
    }

    @Test
    @DisplayName("POST /api/v1/parcels with invalid request returns 400 Bad Request ProblemDetail")
    void createParcelValidationError() throws Exception {
        CreateParcelRequest invalidRequest = new CreateParcelRequest(
                "", // Blank pickup
                "COLOMBO_SOUTH",
                "456 High St, Kandy",
                "INTER_CITY",
                "Nimal Perera",
                "+94771234567",
                ParcelSize.MEDIUM,
                new BigDecimal("-1.00"), // Negative weight
                DeliverySpeed.STANDARD,
                null
        );

        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/parcels without token returns 401 Unauthorized ProblemDetail")
    void createParcelUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v1/parcels with COURIER token returns 403 Forbidden ProblemDetail")
    void createParcelForbiddenForCourier() throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer courier-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/forbidden"));
    }

    @Test
    @DisplayName("GET /api/v1/parcels returns the customer's parcels in the page envelope, newest first by default")
    void getMyParcelsSuccess() throws Exception {
        when(parcelService.listParcels(eq(customerId), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(summary("SD-7K3Q-9P2M")), 0, 20, 1));

        mockMvc.perform(get("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].trackingNo").value("SD-7K3Q-9P2M"))
                .andExpect(jsonPath("$.content[0].history").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(parcelService).listParcels(eq(customerId), eq(false), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("GET /api/v1/parcels passes page, size, sort and status through")
    void getMyParcelsPassesPagingParameters() throws Exception {
        when(parcelService.listParcels(any(), anyBoolean(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 11));

        mockMvc.perform(get("/api/v1/parcels?page=2&size=5&sort=status,asc&status=IN_TRANSIT")
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(parcelService).listParcels(eq(customerId), eq(false), eq(ParcelStatus.IN_TRANSIT), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "status"));
    }

    @Test
    @DisplayName("GET /api/v1/parcels caps the page size at 100")
    void getMyParcelsCapsPageSize() throws Exception {
        when(parcelService.listParcels(any(), anyBoolean(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 100, 0));

        mockMvc.perform(get("/api/v1/parcels?size=500")
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(parcelService).listParcels(any(), anyBoolean(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @ParameterizedTest(name = "GET /api/v1/parcels?{0} returns 400 for field {1}")
    @CsvSource(delimiter = '|', value = {
            "sort=recipientPhone|sort",
            "sort=createdAt,sideways|sort",
            "sort=createdAt,desc,extra|sort",
            "page=-1|page",
            "size=0|size"
    })
    void getMyParcelsRejectsInvalidPaging(String query, String field) throws Exception {
        mockMvc.perform(get("/api/v1/parcels?" + query)
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(parcelService);
    }

    @ParameterizedTest(name = "{0} sees every parcel")
    @ValueSource(strings = {"admin-token", "dispatcher-token"})
    void privilegedCallersListAllParcels(String token) throws Exception {
        when(parcelService.listParcels(any(), eq(true), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/parcels")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(parcelService).listParcels(any(), eq(true), isNull(), any(Pageable.class));
    }

    @ParameterizedTest(name = "GET {0} without token returns 401")
    @ValueSource(strings = {
            "/api/v1/parcels",
            "/api/v1/parcels/c41d0000-2222-4c2e-9a71-0d6f2b9c1e44"
    })
    void readEndpointsRequireToken(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("A token whose subject is not a user ID is rejected with 401")
    void rejectsTokenWithoutUserIdSubject() throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer non-uuid-subject-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/invalid-token"));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("POST /api/v1/parcels with MERCHANT token returns 403 (merchants use the merchant API)")
    void createParcelForbiddenForMerchant() throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer merchant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("+94771234567", new BigDecimal("2.50")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/forbidden"));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("POST /api/v1/parcels returns 503 and creates nothing when pricing is unavailable")
    void createParcelReturns503WhenPricingUnavailable() throws Exception {
        when(parcelService.createCustomerParcel(eq(customerId), any(CreateParcelRequest.class)))
                .thenThrow(new ServiceUnavailableException("pricing-service is not available right now."));

        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/service-unavailable"));
    }

    @ParameterizedTest(name = "{0} = {1} is rejected")
    @CsvSource(nullValues = "NULL", value = {
            "pickupZone, NULL",
            "pickupZone, colombo south",
            "dropoffZone, NULL",
            "dropoffZone, COLOMBO-SOUTH",
            "speed, NULL",
            "speed, TOMORROW",
            "promoCode, DROP 100",
            "promoCode, ''"
    })
    void rejectsMissingOrMalformedRouteFields(String field, String value) throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonWith(field, value)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("POST /api/v1/parcels accepts a booking without a promo code and EXTRA_LARGE parcels")
    void acceptsBookingWithoutPromoCode() throws Exception {
        when(parcelService.createCustomerParcel(eq(customerId), any(CreateParcelRequest.class)))
                .thenReturn(createdResponse());
        Map<String, Object> body = objectMapper.convertValue(validRequest(), new TypeReference<>() {
        });
        body.remove("promoCode");
        body.put("size", "EXTRA_LARGE");

        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "Recipient phone \"{0}\" is accepted")
    @ValueSource(strings = {"+94771234567", "0771234567", "771234567", "+123456789012345"})
    void acceptsValidRecipientPhones(String phone) throws Exception {
        when(parcelService.createCustomerParcel(eq(customerId), any(CreateParcelRequest.class)))
                .thenReturn(createdResponse());

        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(phone, new BigDecimal("2.50")))))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "Recipient phone \"{0}\" is rejected")
    @ValueSource(strings = {
            "077-123-4567",      // separators
            "+94 77 123 4567",   // spaces
            "12345678",          // 8 digits
            "+1234567890123456", // 16 digits
            "++94771234567",     // double plus
            "94771234567+",      // plus not at the start
            "phone-number"
    })
    void rejectsInvalidRecipientPhones(String phone) throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(phone, new BigDecimal("2.50")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("recipientPhone"));

        verifyNoInteractions(parcelService);
    }

    @ParameterizedTest(name = "Weight {0} kg is rejected")
    @ValueSource(strings = {"2.505", "10000.00"})
    void rejectsWeightsThatDoNotFitTheColumn(String weight) throws Exception {
        mockMvc.perform(post("/api/v1/parcels")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("+94771234567", new BigDecimal(weight)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("weightKg"));

        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("GET /api/v1/parcels/{id} returns parcel details for owner")
    void getParcelByIdSuccess() throws Exception {
        ParcelResponse response = new ParcelResponse(
                parcelId,
                "SD-7K3Q-9P2M",
                customerId,
                null,
                null,
                "Pickup",
                "Dropoff",
                "Recipient",
                "+94771234567",
                ParcelSize.SMALL,
                BigDecimal.ONE,
                new BigDecimal("350.00"),
                "LKR",
                ParcelStatus.CREATED,
                null,
                Instant.now(),
                null,
                List.of()
        );

        when(parcelService.getParcelForCustomer(eq(parcelId), eq(customerId), anyBoolean()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/parcels/" + parcelId)
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parcelId.toString()))
                .andExpect(jsonPath("$.trackingNo").value("SD-7K3Q-9P2M"));
    }

    @Test
    @DisplayName("GET /api/v1/parcels/{id} returns 404 when customer does not own parcel")
    void getParcelByIdForbiddenReturnsNotFound() throws Exception {
        when(parcelService.getParcelForCustomer(eq(parcelId), eq(customerId), anyBoolean()))
                .thenThrow(new NotFoundException("Parcel not found: " + parcelId));

        mockMvc.perform(get("/api/v1/parcels/" + parcelId)
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/not-found"));
    }
}
