package lk.swiftdrop.parcel.tracking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code swiftdrop.tracking.sse.*} limits for live tracking streams. The heartbeat interval is read
 * directly by {@code @Scheduled} from {@code swiftdrop.tracking.sse.heartbeat-interval-ms}.
 *
 * @param maxPerTrackingNo open streams allowed for one tracking number
 * @param maxTotal         open streams allowed on this instance
 * @param timeout          how long a stream stays open; browsers reconnect automatically
 */
@ConfigurationProperties("swiftdrop.tracking.sse")
public record TrackingSseProperties(
        @DefaultValue("10") int maxPerTrackingNo,
        @DefaultValue("500") int maxTotal,
        @DefaultValue("30m") Duration timeout) {
}
