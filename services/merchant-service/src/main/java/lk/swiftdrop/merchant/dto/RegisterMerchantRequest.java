package lk.swiftdrop.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterMerchantRequest(
        @NotBlank(message = "Business name is required")
        @Size(max = 200, message = "Business name must not exceed 200 characters")
        String businessName,

        @NotBlank(message = "Business registration number is required")
        @Size(max = 100, message = "Business registration number must not exceed 100 characters")
        String businessRegNo,

        @NotBlank(message = "Contact name is required")
        @Size(max = 150, message = "Contact name must not exceed 150 characters")
        String contactName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phone,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address
) {}
