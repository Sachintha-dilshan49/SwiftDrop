package lk.swiftdrop.notification.notification.channel;

public interface NotificationChannel {
    void send(String recipient, String subject, String body);
    String channelName();
}
