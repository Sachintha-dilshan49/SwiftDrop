package lk.swiftdrop.auth.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAccountsSeederTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);

    @Test
    void createsNothingWithoutAPassword() {
        new DemoAccountsSeeder(users, roles, mock(PasswordEncoder.class), "").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void createsNothingWithAPasswordShorterThanSignupAllows() {
        new DemoAccountsSeeder(users, roles, mock(PasswordEncoder.class), "short").run(null);

        verify(users, never()).save(any());
    }
}
