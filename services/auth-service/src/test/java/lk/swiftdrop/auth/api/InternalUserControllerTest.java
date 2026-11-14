package lk.swiftdrop.auth.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InternalUserController.class)
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void returnsContactResponseForAuthenticatedCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.getUserContact(userId))
                .thenReturn(new UserContactResponse("demo@example.test", "+94771234567", "Demo"));

        mockMvc.perform(get("/internal/users/{userId}/contact", userId)
                        .header(InternalTokenAuthenticationFilter.HEADER, InternalTokenTestContextCustomizerFactory.TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@example.test"))
                .andExpect(jsonPath("$.phone").value("+94771234567"))
                .andExpect(jsonPath("$.firstName").value("Demo"));
    }

    @Test
    void rejectsAnonymousContactLookup() throws Exception {
        mockMvc.perform(get("/internal/users/{userId}/contact", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void grantsRoleForAuthenticatedCaller() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/internal/users/{userId}/roles", userId)
                        .header(InternalTokenAuthenticationFilter.HEADER, InternalTokenTestContextCustomizerFactory.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MERCHANT\"}"))
                .andExpect(status().isNoContent());

        verify(authService).grantRole(userId, RoleName.MERCHANT);
    }

    @Test
    void rejectsAnonymousGrantRole() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/internal/users/{userId}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MERCHANT\"}"))
                .andExpect(status().isUnauthorized());
    }
}
