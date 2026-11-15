package lk.swiftdrop.dispatch.report;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.dispatch.config.ClockConfig;
import lk.swiftdrop.dispatch.courier.AuthServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    /** Default range when none is given: the last seven days, today included. */
    static final int DEFAULT_DAYS = 7;
    /** Longest range one request may cover. */
    static final int MAX_DAYS = 92;

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final AuthServiceClient authServiceClient;
    private final Clock clock;

    public ReportService(ReportRepository reportRepository, AuthServiceClient authServiceClient, Clock clock) {
        this.reportRepository = reportRepository;
        this.authServiceClient = authServiceClient;
        this.clock = clock;
    }

    /** {@code from} and {@code to} are Sri Lanka dates, both included; either may be {@code null}. */
    @Transactional(readOnly = true)
    public DailyReportResponse daily(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(ClockConfig.BUSINESS_ZONE));
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1);
        if (start.isAfter(end)) {
            throw new ValidationException("The start date must not be after the end date.",
                    List.of(new FieldErrorDetail("from", "must not be after to")));
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new ValidationException("A report can cover at most " + MAX_DAYS + " days.",
                    List.of(new FieldErrorDetail("to", "must be at most " + MAX_DAYS + " days after from")));
        }

        List<ReportRepository.DayCourierCount> counts = reportRepository.countByDayAndCourier(
                start.atStartOfDay(ClockConfig.BUSINESS_ZONE).toInstant(),
                end.plusDays(1).atStartOfDay(ClockConfig.BUSINESS_ZONE).toInstant());

        Map<LocalDate, long[]> perDay = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            perDay.put(day, new long[3]);
        }
        Map<UUID, long[]> perCourier = new LinkedHashMap<>();
        long[] totals = new long[3];
        for (ReportRepository.DayCourierCount count : counts) {
            long[] values = {count.getAssigned(), count.getDelivered(), count.getFailed()};
            add(perDay.computeIfAbsent(count.getDay(), d -> new long[3]), values);
            add(perCourier.computeIfAbsent(count.getCourierId(), c -> new long[3]), values);
            add(totals, values);
        }

        List<DailyReportResponse.Day> days = perDay.entrySet().stream()
                .map(e -> new DailyReportResponse.Day(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
        return new DailyReportResponse(start, end, days,
                new DailyReportResponse.Counts(totals[0], totals[1], totals[2]), couriers(perCourier));
    }

    /**
     * Most deliveries first, with first names. Names are a nicety, as in the workload: if auth-service is down the
     * report is still returned without them.
     */
    private List<DailyReportResponse.CourierCounts> couriers(Map<UUID, long[]> perCourier) {
        Map<UUID, UUID> userIds = new LinkedHashMap<>();
        if (!perCourier.isEmpty()) {
            reportRepository.findUsers(new ArrayList<>(perCourier.keySet()))
                    .forEach(user -> userIds.put(user.getCourierId(), user.getUserId()));
        }
        List<DailyReportResponse.CourierCounts> rows = new ArrayList<>();
        boolean namesAvailable = true;
        for (Map.Entry<UUID, long[]> entry : perCourier.entrySet()) {
            String firstName = null;
            UUID userId = userIds.get(entry.getKey());
            if (namesAvailable && userId != null) {
                try {
                    firstName = authServiceClient.firstName(userId).orElse(null);
                } catch (ServiceUnavailableException e) {
                    log.warn("Courier names unavailable, showing the report without them");
                    namesAvailable = false;
                }
            }
            long[] v = entry.getValue();
            rows.add(new DailyReportResponse.CourierCounts(entry.getKey(), firstName, v[0], v[1], v[2]));
        }
        rows.sort(Comparator.comparingLong(DailyReportResponse.CourierCounts::delivered).reversed()
                .thenComparing(Comparator.comparingLong(DailyReportResponse.CourierCounts::assigned).reversed()));
        return rows;
    }

    private static void add(long[] target, long[] values) {
        for (int i = 0; i < target.length; i++) {
            target[i] += values[i];
        }
    }
}
