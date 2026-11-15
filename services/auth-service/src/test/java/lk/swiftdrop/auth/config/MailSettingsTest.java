package lk.swiftdrop.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** The mail settings in application.yml: Mailpit by default, MAIL_* shared, AUTH_MAIL_* for reset emails only. */
class MailSettingsTest {

    private static StandardEnvironment environment(Map<String, Object> variables) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new MapPropertySource("variables", variables));
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(source -> environment.getPropertySources().addLast(source));
        return environment;
    }

    @Test
    void defaultsToMailpitWithoutLogin() throws IOException {
        var env = environment(Map.of());
        assertThat(env.getProperty("spring.mail.host")).isEqualTo("localhost");
        assertThat(env.getProperty("spring.mail.port")).isEqualTo("1025");
        assertThat(env.getProperty("spring.mail.username")).isEmpty();
        assertThat(env.getProperty("spring.mail.properties.mail.smtp.auth")).isEqualTo("false");
        assertThat(env.getProperty("spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo("false");
    }

    @Test
    void usesSharedMailServerSettings() throws IOException {
        var env = environment(Map.of("MAIL_HOST", "mail.example.test", "MAIL_USERNAME", "sender",
                "MAIL_SMTP_AUTH", "true", "MAIL_SMTP_STARTTLS", "true"));
        assertThat(env.getProperty("spring.mail.host")).isEqualTo("mail.example.test");
        assertThat(env.getProperty("spring.mail.username")).isEqualTo("sender");
        assertThat(env.getProperty("spring.mail.properties.mail.smtp.auth")).isEqualTo("true");
        assertThat(env.getProperty("spring.mail.properties.mail.smtp.starttls.required")).isEqualTo("true");
    }

    @Test
    void authMailSettingsOverrideSharedOnesForResetEmails() throws IOException {
        var env = environment(Map.of("MAIL_HOST", "mailpit", "MAIL_PORT", "1025",
                "AUTH_MAIL_HOST", "smtp.gmail.com", "AUTH_MAIL_PORT", "587", "AUTH_MAIL_USERNAME", "sender",
                "AUTH_MAIL_PASSWORD", "app-password", "AUTH_MAIL_SMTP_AUTH", "true", "AUTH_MAIL_SMTP_STARTTLS", "true"));
        assertThat(env.getProperty("spring.mail.host")).isEqualTo("smtp.gmail.com");
        assertThat(env.getProperty("spring.mail.port")).isEqualTo("587");
        assertThat(env.getProperty("spring.mail.password")).isEqualTo("app-password");
        assertThat(env.getProperty("spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo("true");
    }
}
