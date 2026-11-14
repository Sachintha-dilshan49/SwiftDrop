package lk.swiftdrop.auth.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MobileNumberValidator implements ConstraintValidator<ValidMobileNumber, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String number = value.trim();
        if (!number.matches("\\+?[0-9\\s().-]+")) return false;
        long digits = number.chars().filter(character -> character >= '0' && character <= '9').count();
        return digits >= 7 && digits <= 15;
    }
}
