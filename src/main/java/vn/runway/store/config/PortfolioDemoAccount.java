package vn.runway.store.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.model.Account;
import vn.runway.store.repository.AccountRepository;

@Component
@Profile("portfolio-demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
class PortfolioDemoAccount implements CommandLineRunner {
  private final ConfigurableEnvironment environment;
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;

  PortfolioDemoAccount(
      ConfigurableEnvironment environment, AccountRepository accounts, PasswordEncoder encoder) {
    this.environment = environment;
    this.accounts = accounts;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!Arrays.asList(environment.getActiveProfiles()).contains("portfolio-demo")
        || environment.matchesProfiles("prod", "mysql")
        || !environment.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:")) {
      throw new IllegalStateException("Portfolio demo requires an explicit profile and in-memory H2.");
    }

    var variables = environment.getSystemEnvironment();
    String email =
        ((String) variables.getOrDefault("RUNWAY_PORTFOLIO_DEMO_EMAIL", ""))
            .strip()
            .toLowerCase(Locale.ROOT);
    String password = (String) variables.getOrDefault("RUNWAY_PORTFOLIO_DEMO_PASSWORD", "");
    int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
    if (email.length() > 150
        || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
        || password.isBlank()
        || password.length() < 8
        || passwordBytes > 72) {
      throw new IllegalStateException(
          "Set a valid portfolio demo email and password (8 characters, at most 72 UTF-8 bytes).");
    }

    var existing = accounts.findByEmail(email);
    if (existing.isPresent()) {
      if (!"CUSTOMER".equals(existing.get().role)
          || !encoder.matches(password, existing.get().password)) {
        throw new IllegalStateException(
            "Portfolio demo account conflicts with an existing account. Use a fresh demo database.");
      }
      return;
    }

    var account = new Account();
    account.email = email;
    account.name = "Khách hàng portfolio";
    account.password = encoder.encode(password);
    account.role = "CUSTOMER";
    accounts.save(account);
  }
}
