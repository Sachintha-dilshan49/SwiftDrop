package lk.swiftdrop.auth.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AdminUserResponse(
                userId, "Demo User", "demo@example.test", List.of("CUSTOMER"), AccountStatus.ACTIVE);
        when(adminUserService.listUsers(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.content[0].accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanChangeRolesAndDisableUser() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AdminUserResponse(
                userId, "Demo User", "demo@example.test", List.of("MERCHANT"), AccountStatus.DISABLED);
        when(adminUserService.updateUser(
                        userId,
                        new AdminUserUpdateRequest(Set.of(RoleName.MERCHANT), AccountStatus.DISABLED)))
                .thenReturn(user);

        mockMvc.perform(patch("/api/v1/admin/users/{userId}", userId)
                        .contentType("application/json")
                        .content("""
                                {"roles":["MERCHANT"],"accountStatus":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("MERCHANT"))
                .andExpect(jsonPath("$.accountStatus").value("DISABLED"));
        verify(adminUserService).updateUser(
                userId,
                new AdminUserUpdateRequest(Set.of(RoleName.MERCHANT), AccountStatus.DISABLED));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void nonAdminCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidRoleUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{userId}", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"roles":[],"accountStatus":"DISABLED"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
