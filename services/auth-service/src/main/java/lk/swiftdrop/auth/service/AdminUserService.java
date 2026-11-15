package lk.swiftdrop.auth.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.auth.account.RoleEntity;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.api.AdminUserResponse;
import lk.swiftdrop.auth.api.AdminUserUpdateRequest;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.common.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable boundedPage = PageRequest.of(
                pageable.getPageNumber(),
                pageSize,
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id").ascending());
        return userRepository.findAll(boundedPage).map(AdminUserService::toResponse);
    }

    @Transactional
    public AdminUserResponse updateUser(UUID userId, AdminUserUpdateRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        List<RoleEntity> roles = request.roles().stream()
                .sorted()
                .map(this::findRole)
                .toList();
        user.replaceRoles(roles);
        user.setAccountStatus(request.accountStatus());
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId, clock.instant());
        return toResponse(user);
    }

    private RoleEntity findRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Required role is missing from auth_db: " + name));
    }

    private static AdminUserResponse toResponse(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roles,
                user.getAccountStatus());
    }
}
