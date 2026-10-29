package lk.swiftdrop.common.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class ProblemTestController {

    record CreateRequest(@NotBlank String trackingNo, @Positive BigDecimal weightKg) {
    }

    @GetMapping("/not-found")
    String notFound() {
        throw new NotFoundException("Parcel was not found.");
    }

    @GetMapping("/conflict")
    String conflict() {
        throw new ConflictException("invalid-status-transition", "Invalid status transition",
                "Parcel cannot move from CREATED to DELIVERED.");
    }

    @GetMapping("/forbidden")
    String forbidden() {
        throw new ForbiddenException("This delivery is assigned to another courier.");
    }

    @GetMapping("/unavailable")
    String unavailable() {
        throw new ServiceUnavailableException("pricing-service is not available right now. Please try again later.");
    }

    @GetMapping("/business-validation")
    String businessValidation() {
        throw new ValidationException("Zone is not served.",
                List.of(new FieldErrorDetail("toZone", "is not served")));
    }

    @PostMapping("/parcels")
    String create(@Valid @RequestBody CreateRequest request) {
        return "ok";
    }

    @GetMapping("/parcels/{id}")
    String byId(@PathVariable UUID id) {
        return id.toString();
    }

    @GetMapping("/page")
    String page(@RequestParam @Min(1) int size) {
        return "ok";
    }

    @GetMapping("/service-validation")
    String serviceValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            throw new ConstraintViolationException(validator.validate(new CreateRequest("SD-1", BigDecimal.ZERO)));
        }
    }

    @GetMapping("/boom")
    String boom() {
        throw new IllegalStateException("Lookup failed for jane.doe@example.com on +94 77 123 4567");
    }
}
