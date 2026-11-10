package lk.swiftdrop.parcel.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code swiftdrop.outbox.*} settings for the relay. The polling interval is read directly by
 * {@code @Scheduled} from {@code swiftdrop.outbox.poll-interval-ms}.
 *
 * @param batchSize          events locked and published per run
 * @param confirmTimeout     how long to wait for broker confirms for one batch
 * @param initialBackoff     delay before the first retry; doubles on each further failure
 * @param maxBackoff         upper bound for the retry delay
 * @param alertAfterAttempts failed attempts after which each retry is logged at ERROR instead of WARN
 */
@ConfigurationProperties("swiftdrop.outbox")
public record OutboxProperties(
        @DefaultValue("50") int batchSize,
        @DefaultValue("5s") Duration confirmTimeout,
        @DefaultValue("1s") Duration initialBackoff,
        @DefaultValue("60s") Duration maxBackoff,
        @DefaultValue("20") int alertAfterAttempts) {
}
