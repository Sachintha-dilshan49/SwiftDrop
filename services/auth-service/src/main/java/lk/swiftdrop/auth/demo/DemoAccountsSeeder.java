package lk.swiftdrop.auth.demo;

import java.util.List;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.RoleRepository;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.account.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one staff account per role for demos, so a fresh installation can approve merchants, assign parcels and
 * deliver them. Signup only creates customers, and the first admin cannot be created through the API.
 *
 * <p>Off unless {@code AUTH_DEMO_ACCOUNTS_PASSWORD} is set (local and demo environments only); no password is
 * committed. Existing accounts are left as they are, so it is safe on every start.
 */
@Component
public class DemoAccountsSeeder implements ApplicationRunner {

    /** Same minimum as signup (SignupRequest). */
    static final int MIN_PASSWORD_LENGTH = 12;

    static final List<DemoAccount> ACCOUNTS = List.of(
            new DemoAccount("Demo Admin", "demo.admin@swiftdrop.example.com", RoleName.ADMIN),
            new DemoAccount("Demo Dispatcher", "demo.dispatcher@swiftdrop.example.com", RoleName.DISPATCHER),
            new DemoAccount("Demo Courier", "demo.courier@swiftdrop.example.com", RoleName.COURIER));

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DemoAccountsSeeder(UserRepository userRepository, RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${swiftdrop.demo-accounts.password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("AUTH_DEMO_ACCOUNTS_PASSWORD is shorter than {} characters; demo accounts not created",
                    MIN_PASSWORD_LENGTH);
            return;
        }
        for (DemoAccount account : ACCOUNTS) {
            if (userRepository.existsByEmailIgnoreCase(account.email())) {
                continue;
            }
            UserEntity user = new UserEntity(account.fullName(), account.email(), null, passwordEncoder.encode(password));
            user.addRole(roleRepository.findByName(account.role())
                    .orElseThrow(() -> new IllegalStateException("Role " + account.role() + " is missing from auth_db.")));
            userRepository.save(user);
            log.info("Created demo {} account", account.role());
        }
    }

    record DemoAccount(String fullName, String email, RoleName role) {
    }
}
