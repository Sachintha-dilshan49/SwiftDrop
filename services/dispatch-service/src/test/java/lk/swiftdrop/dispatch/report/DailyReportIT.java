package lk.swiftdrop.dispatch.report;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The report query against PostgreSQL, with Sri Lanka day boundaries. */
@SpringBootTest
@Testcontainers
class DailyReportIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @RegisterExtension
    static WireMockExtension authService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.services.auth-url", authService::baseUrl);
    }

    @Autowired
    private ReportService reportService;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void countsByColomboDayAssignedOnOneDayDeliveredOnTheNext() {
        LocalDate day = LocalDate.of(2025, 3, 10);
        Courier courier = courier("Kasun");
        // Assigned 10 Mar 23:00 Colombo, delivered 11 Mar 00:10 Colombo (still 10 Mar in UTC)
        assignment(courier, AssignmentStatus.COMPLETED, "2025-03-10T17:30:00Z", "2025-03-10T18:40:00Z");
        // Assigned and failed on 10 Mar
        assignment(courier, AssignmentStatus.FAILED, "2025-03-10T05:00:00Z", "2025-03-10T09:00:00Z");
        // Still out on 11 Mar: counts as assigned only
        assignment(courier, AssignmentStatus.IN_PROGRESS, "2025-03-11T04:00:00Z", "2025-03-11T05:00:00Z");
        // Outside the range
        assignment(courier, AssignmentStatus.COMPLETED, "2025-03-09T04:00:00Z", "2025-03-09T08:00:00Z");

        DailyReportResponse report = reportService.daily(day, day.plusDays(1));

        assertThat(report.days()).containsExactly(
                new DailyReportResponse.Day(day, 2, 0, 1),
                new DailyReportResponse.Day(day.plusDays(1), 1, 1, 0));
        assertThat(report.totals()).isEqualTo(new DailyReportResponse.Counts(3, 1, 1));
        assertThat(report.couriers()).singleElement().satisfies(row -> {
            assertThat(row.courierId()).isEqualTo(courier.getId());
            assertThat(row.firstName()).isEqualTo("Kasun");
        });
    }

    private Courier courier(String firstName) {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier.setStatus(CourierStatus.AVAILABLE);
        authService.stubFor(get(urlPathEqualTo("/internal/users/" + courier.getUserId() + "/contact"))
                .willReturn(okJson("{\"firstName\":\"" + firstName + "\"}")));
        return courierRepository.save(courier);
    }

    private void assignment(Courier courier, AssignmentStatus status, String assignedAt, String updatedAt) {
        Assignment assignment = new Assignment();
        assignment.setParcelId(UUID.randomUUID());
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(UUID.randomUUID());
        assignment.setStatus(status);
        Assignment saved = assignmentRepository.save(assignment);
        jdbcTemplate.update("update assignments set assigned_at = ?, updated_at = ? where id = ?",
                Timestamp.from(Instant.parse(assignedAt)), Timestamp.from(Instant.parse(updatedAt)), saved.getId());
    }
}
