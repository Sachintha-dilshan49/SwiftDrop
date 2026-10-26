package lk.swiftdrop.notification.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock SMS channel — logs the message instead of calling a real SMS API.
 * Replace with Twilio/Vonage adapter in production.
 */
@Component
public class SmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public void send(String recipient, String subject, String body) {
        log.info("SMS sent — body length: {} chars", body.length());
    }

    @Override
    public String channelName() { return "SMS"; }
}
