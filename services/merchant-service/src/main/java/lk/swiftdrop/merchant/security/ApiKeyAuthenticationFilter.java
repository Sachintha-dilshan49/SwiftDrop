package lk.swiftdrop.merchant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.service.ApiKeyService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService,
                                      AuthenticationEntryPoint authenticationEntryPoint) {
        this.apiKeyService = apiKeyService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawApiKey = request.getHeader(API_KEY_HEADER);
        if (rawApiKey == null || rawApiKey.isBlank()) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Missing " + API_KEY_HEADER + " header"));
            return;
        }

        Optional<Merchant> merchantOpt = apiKeyService.validateApiKey(rawApiKey);
        if (merchantOpt.isEmpty()) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Invalid or revoked API key"));
            return;
        }

        Merchant merchant = merchantOpt.get();
        MerchantApiKeyAuthentication auth = new MerchantApiKeyAuthentication(merchant);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
