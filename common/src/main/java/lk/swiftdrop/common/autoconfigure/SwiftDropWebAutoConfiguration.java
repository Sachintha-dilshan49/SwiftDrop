package lk.swiftdrop.common.autoconfigure;

import jakarta.validation.ConstraintViolationException;
import lk.swiftdrop.common.error.ConstraintViolationExceptionHandler;
import lk.swiftdrop.common.error.GlobalExceptionHandler;
import lk.swiftdrop.common.web.RequestIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/** Error handling and request IDs for servlet (Spring MVC) services. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
public class SwiftDropWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ConstraintViolationException.class)
    static class BeanValidationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ConstraintViolationExceptionHandler constraintViolationExceptionHandler() {
            return new ConstraintViolationExceptionHandler();
        }
    }
}
