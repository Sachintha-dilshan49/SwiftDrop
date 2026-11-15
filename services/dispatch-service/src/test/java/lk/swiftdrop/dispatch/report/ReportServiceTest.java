package lk.swiftdrop.dispatch.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.dispatch.courier.AuthServiceClient;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    // 14 Nov 2026, 00:30 in Colombo (UTC+5:30) is still 13 Nov in UTC
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T19:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    private final ReportRepository repository = mock(ReportRepository.class);
    private final AuthServiceClient auth = mock(AuthServiceClient.class);
    private final ReportService service = new ReportService(repository, auth, CLOCK);

    @Test
    void withoutDatesItCoversTheLastSevenSriLankaDays() {
        DailyReportResponse report = service.daily(null, null);

        assertThat(report.to()).isEqualTo(TODAY);
        assertThat(report.from()).isEqualTo(TODAY.minusDays(6));
        assertThat(report.days()).hasSize(7).allSatisfy(day -> assertThat(day.assigned()).isZero());
        // The range starts at Colombo midnight
        verify(repository).countByDayAndCourier(Instant.parse("2026-11-07T18:30:00Z"),
                Instant.parse("2026-11-14T18:30:00Z"));
    }

    @Test
    void addsUpDaysTotalsAndCouriersMostDeliveriesFirst() {
        UUID kasun = UUID.randomUUID();
        UUID nadeesha = UUID.randomUUID();
        when(repository.countByDayAndCourier(any(), any())).thenReturn(List.of(
                count(TODAY.minusDays(1), kasun, 3, 2, 1),
                count(TODAY, kasun, 1, 1, 0),
                count(TODAY, nadeesha, 4, 4, 0)));
        when(repository.findUsers(any())).thenReturn(List.of(user(kasun), user(nadeesha)));
        when(auth.firstName(any())).thenReturn(Optional.of("Kasun"), Optional.of("Nadeesha"));

        DailyReportResponse report = service.daily(TODAY.minusDays(2), TODAY);

        assertThat(report.days()).extracting(DailyReportResponse.Day::delivered).containsExactly(0L, 2L, 5L);
        assertThat(report.totals()).isEqualTo(new DailyReportResponse.Counts(8, 7, 1));
        assertThat(report.couriers()).extracting(DailyReportResponse.CourierCounts::courierId)
                .containsExactly(nadeesha, kasun);
        assertThat(report.couriers()).extracting(DailyReportResponse.CourierCounts::firstName)
                .containsExactly("Nadeesha", "Kasun");
    }

    @Test
    void reportStillWorksWhenNamesAreUnavailable() {
        UUID courier = UUID.randomUUID();
        when(repository.countByDayAndCourier(any(), any())).thenReturn(List.of(count(TODAY, courier, 1, 1, 0)));
        when(repository.findUsers(any())).thenReturn(List.of(user(courier)));
        when(auth.firstName(any())).thenThrow(new ServiceUnavailableException("auth down"));

        DailyReportResponse report = service.daily(TODAY, TODAY);

        assertThat(report.couriers()).singleElement().satisfies(row -> {
            assertThat(row.firstName()).isNull();
            assertThat(row.delivered()).isEqualTo(1);
        });
    }

    @Test
    void startAfterEndIsRejected() {
        assertThatThrownBy(() -> service.daily(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must not be after");
    }

    @Test
    void rangeLongerThanTheLimitIsRejected() {
        assertThatThrownBy(() -> service.daily(TODAY.minusDays(ReportService.MAX_DAYS), TODAY))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at most");
        assertThat(service.daily(TODAY.minusDays(ReportService.MAX_DAYS - 1), TODAY).days())
                .hasSize(ReportService.MAX_DAYS);
    }

    private static ReportRepository.DayCourierCount count(LocalDate day, UUID courier, long assigned, long delivered,
                                                          long failed) {
        return new ReportRepository.DayCourierCount() {
            public LocalDate getDay() { return day; }
            public UUID getCourierId() { return courier; }
            public long getAssigned() { return assigned; }
            public long getDelivered() { return delivered; }
            public long getFailed() { return failed; }
        };
    }

    private static ReportRepository.CourierUser user(UUID courier) {
        UUID userId = UUID.randomUUID();
        return new ReportRepository.CourierUser() {
            public UUID getCourierId() { return courier; }
            public UUID getUserId() { return userId; }
        };
    }
}
