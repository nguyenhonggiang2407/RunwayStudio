package vn.runway.store.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.model.Account;
import vn.runway.store.repository.AccountRepository;

/**
 * Explicit one-off provisioning. Remove the environment password after first successful startup.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;

  @Value("${RUNWAY_BOOTSTRAP_ENABLED:false}")
  boolean enabled;

  @Value("${RUNWAY_BOOTSTRAP_EMAIL:}")
  String email;

  @Value("${RUNWAY_BOOTSTRAP_PASSWORD:}")
  String password;

  public AdminBootstrap(AccountRepository accounts, PasswordEncoder encoder) {
    this.accounts = accounts;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) return;
    String normalized = email.strip().toLowerCase(Locale.ROOT);
    int bytes = password.getBytes(StandardCharsets.UTF_8).length;
    if (!normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
        || normalized.length() > 150
        || bytes < 16
        || bytes > 72)
      throw new IllegalStateException(
          "Explicit bootstrap email and password (16–72 UTF-8 bytes) are required.");
    var existing = accounts.findByEmail(normalized);
    if (existing.isPresent()) {
      if (!existing.get().role.equals("ADMIN"))
        throw new IllegalStateException("Bootstrap cannot promote an existing customer.");
      return;
    }
    var a = new Account();
    a.email = normalized;
    a.name = "Studio Administrator";
    a.password = encoder.encode(password);
    a.role = "ADMIN";
    accounts.save(a);
  }
}
