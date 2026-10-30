package lk.swiftdrop.merchant.security;

import java.util.Collection;
import java.util.List;
import lk.swiftdrop.merchant.entity.Merchant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class MerchantApiKeyAuthentication implements Authentication {

    private final Merchant merchant;
    private boolean authenticated = true;

    public MerchantApiKeyAuthentication(Merchant merchant) {
        this.merchant = merchant;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Merchant getPrincipal() {
        return merchant;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return merchant.getId().toString();
    }
}
