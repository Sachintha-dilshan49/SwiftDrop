package lk.swiftdrop.dispatch.assignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(AssignmentController.class)
class AssignmentControllerTest {

    private static final UUID PARCEL_ID = UUID.randomUUID();
    private static final UUID COURIER_ID = UUID.randomUUID();
    private static final UUID DISPATCHER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void dispatcherAssignsParcelAndGets201WithLocation() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        given(assignmentService.assign(PARCEL_ID, COURIER_ID, DISPATCHER_ID)).willReturn(new AssignmentResponse(
                assignmentId, PARCEL_ID, COURIER_ID, DISPATCHER_ID, AssignmentStatus.ASSIGNED, Instant.now()));

        mockMvc.perform(assignRequest(validBody()).with(user("DISPATCHER")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/dispatch/assignments/" + assignmentId))
                .andExpect(jsonPath("$.id").value(assignmentId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedByUserId").value(DISPATCHER_ID.toString()));
    }

    @Test
    void adminCanAssignParcels() throws Exception {
        given(assignmentService.assign(PARCEL_ID, COURIER_ID, DISPATCHER_ID)).willReturn(new AssignmentResponse(
                UUID.randomUUID(), PARCEL_ID, COURIER_ID, DISPATCHER_ID, AssignmentStatus.ASSIGNED, Instant.now()));

        mockMvc.perform(assignRequest(validBody()).with(user("ADMIN")))
                .andExpect(status().isCreated());
    }

    @Test
    void courierCannotAssignParcels() throws Exception {
        mockMvc.perform(assignRequest(validBody()).with(user("COURIER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assignmentService);
    }

    @Test
    void customerCannotAssignParcels() throws Exception {
        mockMvc.perform(assignRequest(validBody()).with(user("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assignmentService);
    }

    @Test
    void rejectsRequestWithoutTokenWith401() throws Exception {
        mockMvc.perform(assignRequest(validBody()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(assignmentService);
    }

    @Test
    void rejectsMissingParcelIdWith400() throws Exception {
        mockMvc.perform(assignRequest("{\"courierId\":\"" + COURIER_ID + "\"}").with(user("DISPATCHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("parcelId"));
        verifyNoInteractions(assignmentService);
    }

    @Test
    void unknownCourierReturns404() throws Exception {
        given(assignmentService.assign(any(), any(), any()))
                .willThrow(new NotFoundException("courier-not-found", "Courier not found", "Courier does not exist."));

        mockMvc.perform(assignRequest(validBody()).with(user("DISPATCHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/courier-not-found"));
    }

    @Test
    void alreadyAssignedParcelReturns409() throws Exception {
        given(assignmentService.assign(any(), any(), any())).willThrow(new ConflictException(
                "parcel-already-assigned", "Parcel already assigned", "Parcel already has an active assignment."));

        mockMvc.perform(assignRequest(validBody()).with(user("DISPATCHER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/parcel-already-assigned"));
    }

    private static MockHttpServletRequestBuilder assignRequest(String body) {
        return post("/api/v1/dispatch/assignments").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String validBody() {
        return "{\"parcelId\":\"" + PARCEL_ID + "\",\"courierId\":\"" + COURIER_ID + "\"}";
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject(DISPATCHER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
