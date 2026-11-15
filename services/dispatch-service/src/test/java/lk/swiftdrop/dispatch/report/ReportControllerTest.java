package lk.swiftdrop.dispatch.report;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 14);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void dispatcherGetsTheDailyReportForTheRange() throws Exception {
        given(reportService.daily(DAY, DAY)).willReturn(new DailyReportResponse(DAY, DAY,
                List.of(new DailyReportResponse.Day(DAY, 5, 4, 1)), new DailyReportResponse.Counts(5, 4, 1),
                List.of(new DailyReportResponse.CourierCounts(UUID.randomUUID(), "Kasun", 5, 4, 1))));

        mockMvc.perform(get("/api/v1/reports/daily?from=2026-11-14&to=2026-11-14").with(user("DISPATCHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].date").value("2026-11-14"))
                .andExpect(jsonPath("$.days[0].delivered").value(4))
                .andExpect(jsonPath("$.totals.failed").value(1))
                .andExpect(jsonPath("$.couriers[0].firstName").value("Kasun"));
    }

    @Test
    void adminCanViewReports() throws Exception {
        given(reportService.daily(null, null)).willReturn(new DailyReportResponse(DAY, DAY, List.of(),
                new DailyReportResponse.Counts(0, 0, 0), List.of()));

        mockMvc.perform(get("/api/v1/reports/daily").with(user("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void reversedRangeIsAValidationProblem() throws Exception {
        given(reportService.daily(DAY, DAY.minusDays(1))).willThrow(new ValidationException(
                "The start date must not be after the end date.", List.of(new FieldErrorDetail("from", "bad"))));

        mockMvc.perform(get("/api/v1/reports/daily?from=2026-11-14&to=2026-11-13").with(user("DISPATCHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The start date must not be after the end date."));
    }

    @Test
    void dateThatIsNotADateIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/reports/daily?from=yesterday").with(user("DISPATCHER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reportService);
    }

    @Test
    void courierCannotViewReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/daily").with(user("COURIER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportService);
    }

    @Test
    void rejectsRequestWithoutTokenWith401() throws Exception {
        mockMvc.perform(get("/api/v1/reports/daily"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reportService);
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
