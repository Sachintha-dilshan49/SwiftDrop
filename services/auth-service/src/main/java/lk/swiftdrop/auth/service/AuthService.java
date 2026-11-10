package lk.swiftdrop.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.api.LoginRequest;
import lk.swiftdrop.auth.api.SignupRequest;
import lk.swiftdrop.auth.api.UserContactResponse;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.RefreshTokenEntity;
import lk.swiftdrop.auth.token.RefreshTokenRepository;
import lk.swiftdrop.auth.token.TokenResponse;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.UnauthorizedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Email or mobile number and password are invalid.";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateAccountConflict();
        }
        var customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Required CUSTOMER role is missing from auth_db."));
        UserEntity user = new UserEntity(
                request.fullName().trim(),
                email,
                normalizeMobileNumber(request.mobileNumber()),
                passwordEncoder.encode(request.password()));
        user.addRole(customerRole);
        try {
            return issueTokens(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateAccountConflict();
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String identifier = request.emailOrMobile().trim();
        UserEntity user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> isPhoneIdentifier(identifier)
                        ? userRepository.findByMobileNumber(normalizeMobileNumber(identifier))
                        : Optional.empty())
                .filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(candidate -> candidate.getPasswordHash() != null
                        && passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = tokenService.hashRefreshToken(rawRefreshToken);
        RefreshTokenEntity oldToken = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired."));
        Instant now = clock.instant();
        if (oldToken.getRevokedAt() != null || !oldToken.getExpiresAt().isAfter(now)
                || oldToken.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Refresh token is invalid or expired.");
        }

        UserEntity user = oldToken.getUser();
        String replacement = tokenService.createRefreshToken();
        RefreshTokenEntity replacementEntity = new RefreshTokenEntity(
                user,
                tokenService.hashRefreshToken(replacement),
                now.plus(JwtTokenService.REFRESH_TOKEN_TTL));
        RefreshTokenEntity savedReplacement = refreshTokenRepository.save(replacementEntity);
        oldToken.revoke(now, savedReplacement.getId());
        return response(user, replacement);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(tokenService.hashRefreshToken(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(clock.instant(), null));
    }

    @Transactional
    public TokenResponse issueForAuthenticatedUser(UserEntity user) {
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserContactResponse getUserContact(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        String firstName = user.getFullName().trim().split("\\s+", 2)[0];
        return new UserContactResponse(user.getEmail(), user.getMobileNumber(), firstName);
    }

    private TokenResponse issueTokens(UserEntity user) {
        String refreshToken = tokenService.createRefreshToken();
        refreshTokenRepository.save(new RefreshTokenEntity(
                user,
                tokenService.hashRefreshToken(refreshToken),
                clock.instant().plus(JwtTokenService.REFRESH_TOKEN_TTL)));
        return response(user, refreshToken);
    }

    private TokenResponse response(UserEntity user, String refreshToken) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
        Instant now = clock.instant();
        return new TokenResponse(
                tokenService.createAccessToken(user),
                now.plus(JwtTokenService.ACCESS_TOKEN_TTL),
                refreshToken,
                new TokenResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail(), roles));
    }

    private static ConflictException duplicateAccountConflict() {
        return new ConflictException("account-already-registered", "Account already registered",
                "An account with this email or mobile number already exists.");
    }

    private static String normalizeMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return null;
        }
        return mobileNumber.replaceAll("[\\s().-]", "");
    }

    private static boolean isPhoneIdentifier(String identifier) {
        return identifier.matches("[+0-9\\s().-]+") && identifier.chars().anyMatch(Character::isDigit);
    }
}
