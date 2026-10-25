package lk.swiftdrop.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lk.swiftdrop.common.web.RequestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProblemTestController.class)
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mapsNotFoundExceptionToProblemDetailWithRequestId() throws Exception {
        mockMvc.perform(get("/test/not-found").header(RequestIds.HEADER, "req-123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(RequestIds.HEADER, "req-123"))
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Parcel was not found."))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.requestId").value("req-123"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void usesSpecificProblemTypeForConflicts() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/invalid-status-transition"))
                .andExpect(jsonPath("$.title").value("Invalid status transition"))
                .andExpect(jsonPath("$.detail").value("Parcel cannot move from CREATED to DELIVERED."));
    }

    @Test
    void mapsForbiddenExceptionTo403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/forbidden"));
    }

    @Test
    void includesFieldErrorsFromDomainValidationException() throws Exception {
        mockMvc.perform(get("/test/business-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("toZone"))
                .andExpect(jsonPath("$.errors[0].message").value("is not served"));
    }

    @Test
    void listsEveryInvalidFieldOfRequestBodyWithoutRejectedValues() throws Exception {
        mockMvc.perform(post("/test/parcels").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNo\":\" \",\"weightKg\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value("/test/parcels"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("trackingNo", "weightKg")))
                .andExpect(jsonPath("$.errors[*].rejectedValue").doesNotExist());
    }

    @Test
    void mapsHandlerMethodValidationToFieldErrors() throws Exception {
        mockMvc.perform(get("/test/page").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void mapsConstraintViolationsFromServiceLayerTo400() throws Exception {
        mockMvc.perform(get("/test/service-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("weightKg"));
    }

    @Test
    void completesSpringMvcProblemsWithSwiftDropTypeAndRequestId() throws Exception {
        mockMvc.perform(get("/test/parcels/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/bad-request"))
                .andExpect(jsonPath("$.instance").value("/test/parcels/not-a-uuid"))
                .andExpect(jsonPath("$.requestId").exists());

        mockMvc.perform(post("/test/not-found").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/method-not-allowed"));
    }

    @Test
    void hidesMessageAndStackTraceOfUnexpectedExceptions() throws Exception {
        String body = mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/internal-error"))
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.INTERNAL_DETAIL))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("jane.doe", "+94", "IllegalStateException");
    }
}
