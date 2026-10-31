package lk.swiftdrop.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.OAuthAccountEntity;
import lk.swiftdrop.auth.account.OAuthAccountRepository;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.OAuthLoginCodeEntity;
import lk.swiftdrop.auth.token.OAuthLoginCodeRepository;
import lk.swiftdrop.auth.token.TokenResponse;
import lk.swiftdrop.common.error.UnauthorizedException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuthService {

    private static final Duration LOGIN_CODE_TTL = Duration.ofMinutes(2);
    private static final String GOOGLE_PROVIDER = "google";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;
    private final JwtTokenService tokenService;
    private final AuthService authService;
    private final Clock clock;

    public GoogleOAuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OAuthAccountRepository oauthAccountRepository,
            OAuthLoginCodeRepository loginCodeRepository,
            JwtTokenService tokenService,
            AuthService authService,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.tokenService = tokenService;
        this.authService = authService;
        this.clock = clock;
    }

    @Transactional
    public String createLoginCode(OAuth2User googleUser) {
        String providerUserId = requiredAttribute(googleUser, "sub");
        String email = requiredAttribute(googleUser, "email").trim().toLowerCase(java.util.Locale.ROOT);
        String fullName = requiredAttribute(googleUser, "name");
        if (!Boolean.TRUE.equals(googleUser.getAttribute("email_verified"))) {
            throw new UnauthorizedException("Google account email must be verified.");
        }

        UserEntity user = oauthAccountRepository.findByProviderAndProviderUserId(GOOGLE_PROVIDER, providerUserId)
                .map(OAuthAccountEntity::getUser)
                .orElseGet(() -> createOrLinkGoogleUser(providerUserId, email, fullName));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("This account is not available.");
        }

        String rawCode = tokenService.createRefreshToken();
        loginCodeRepository.save(new OAuthLoginCodeEntity(
                user,
                tokenService.hashRefreshToken(rawCode),
                clock.instant().plus(LOGIN_CODE_TTL)));
        return rawCode;
    }

    @Transactional
    public TokenResponse exchangeLoginCode(String rawCode) {
        OAuthLoginCodeEntity code = loginCodeRepository
                .findByCodeHashForUpdate(tokenService.hashRefreshToken(rawCode))
                .orElseThrow(() -> new UnauthorizedException("Google login code is invalid or expired."));
        Instant now = clock.instant();
        if (code.getConsumedAt() != null || !code.getExpiresAt().isAfter(now)
                || code.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Google login code is invalid or expired.");
        }
        code.consume(now);
        return authService.issueForAuthenticatedUser(code.getUser());
    }

    private UserEntity createOrLinkGoogleUser(String providerUserId, String email, String fullName) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            var customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                    .orElseThrow(() -> new IllegalStateException("Required CUSTOMER role is missing from auth_db."));
            UserEntity newUser = new UserEntity(fullName, email, null, null);
            newUser.addRole(customerRole);
            return userRepository.saveAndFlush(newUser);
        });
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("This account is not available.");
        }
        oauthAccountRepository.save(new OAuthAccountEntity(user, GOOGLE_PROVIDER, providerUserId));
        return user;
    }

    private static String requiredAttribute(OAuth2User user, String name) {
        Object value = user.getAttribute(name);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new UnauthorizedException("Google did not provide the required profile information.");
    }
}
