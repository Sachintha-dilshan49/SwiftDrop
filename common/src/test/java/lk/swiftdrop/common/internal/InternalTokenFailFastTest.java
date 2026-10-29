package lk.swiftdrop.common.internal;

import static org.assertj.core.api.Assertions.assertThat;

import lk.swiftdrop.common.autoconfigure.SwiftDropInternalSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

// ApplicationContextRunner is not a Spring test context, so no test token is supplied here
class InternalTokenFailFastTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class, SwiftDropInternalSecurityAutoConfiguration.class));

    @Test
    void failsAtStartupWhenTokenIsNotSet() {
        runner.run(context -> assertThat(context).hasFailed()
                .getFailure().rootCause().hasMessageContaining("INTERNAL_API_TOKEN"));
    }

    @Test
    void failsAtStartupWhenTokenIsBlankOrAnUnresolvedPlaceholder() {
        runner.withPropertyValues("swiftdrop.internal.token=  ")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("swiftdrop.internal.token=${INTERNAL_API_TOKEN}")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startsWithInternalChainWhenTokenIsSet() {
        runner.withPropertyValues("swiftdrop.internal.token=0123456789abcdef")
                .run(context -> assertThat(context).hasNotFailed()
                        .hasBean("swiftDropInternalFilterChain")
                        .hasSingleBean(InternalRestClientFactory.class)
                        .getBeans(SecurityFilterChain.class).isNotEmpty());
    }

    @Test
    void propertiesNeverPrintTheToken() {
        assertThat(new SwiftDropInternalProperties("s3cr3t-token")).asString().doesNotContain("s3cr3t-token");
    }
}
