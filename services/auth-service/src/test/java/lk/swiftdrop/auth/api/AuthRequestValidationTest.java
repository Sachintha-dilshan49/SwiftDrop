package lk.swiftdrop.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.auth.reset.PasswordResetService;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.auth.service.GoogleOAuthService;
import lk.swiftdrop.auth.token.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthRequestValidationTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private GoogleOAuthService googleOAuthService;
    @MockitoBean private PasswordResetService passwordResetService;

    @Test
    void signupAcceptsValidPhoneAndPassword() throws Exception {
        var user = new TokenResponse.UserSummary(UUID.randomUUID(), "Demo User", "demo@example.test", List.of("CUSTOMER"));
        when(authService.signup(any())).thenReturn(new TokenResponse("access", Instant.now(), "refresh", user));
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Demo User","email":"demo@example.test",
                                 "mobileNumber":"+94 77 123 4567","password":"long-enough-password"}
                                """))
                .andExpect(status().isCreated());
        verify(authService).signup(any());
    }

    @Test
    void signupRejectsMalformedPhoneBeforeCreatingAccount() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Demo User","email":"demo@example.test",
                                 "mobileNumber":"abc123","password":"long-enough-password"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void signupRejectsPasswordExceedingBcryptByteLimit() throws Exception {
        String password = "é".repeat(37);
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Demo User","email":"demo@example.test","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void loginRejectsBlankCredentialsBeforeAuthenticating() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailOrMobile\":\"   \",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void forgotPasswordAnswersAcceptedAndStartsReset() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@example.test\"}"))
                .andExpect(status().isAccepted());
        verify(passwordResetService).requestReset("demo@example.test");
    }

    @Test
    void forgotPasswordRejectsMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPasswordSetsNewPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"newPassword\":\"another-long-password\"}"))
                .andExpect(status().isNoContent());
        verify(passwordResetService).resetPassword("reset-token", "another-long-password");
    }

    @Test
    void resetPasswordRejectsShortPasswordBeforeUsingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPasswordWithExpiredLinkIsRejectedAsProblemDetail() throws Exception {
        doThrow(new ValidationException("This reset link is invalid or has expired. Ask for a new one."))
                .when(passwordResetService).resetPassword("expired", "another-long-password");
        mockMvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"expired\",\"newPassword\":\"another-long-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This reset link is invalid or has expired. Ask for a new one."));
    }
}
