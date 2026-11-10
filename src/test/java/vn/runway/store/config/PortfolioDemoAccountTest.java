package vn.runway.store.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.runway.store.model.Account;
import vn.runway.store.repository.AccountRepository;

class PortfolioDemoAccountTest {
  private static final String EMAIL = "portfolio-test@example.test";
  private final String password = UUID.randomUUID().toString();
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final MockEnvironment environment = spy(new MockEnvironment());
  private final PortfolioDemoAccount seed = new PortfolioDemoAccount(environment, accounts, encoder);

  @BeforeEach
  void configureDisposableDemo() {
    environment.setActiveProfiles("portfolio-demo");
    environment.setProperty("spring.datasource.url", "jdbc:h2:mem:portfolio-fixture");
    doReturn(Map.of(
            "RUNWAY_PORTFOLIO_DEMO_EMAIL", "  PORTFOLIO-TEST@EXAMPLE.TEST  ",
            "RUNWAY_PORTFOLIO_DEMO_PASSWORD", password))
        .when(environment).getSystemEnvironment();
  }

  @Test
  void createsOneRestrictedAccountAndDoesNotOverwriteIt() {
    seed.run();
    var saved = ArgumentCaptor.forClass(Account.class);
    verify(accounts).save(saved.capture());
    var account = saved.getValue();
    assertThat(account.email).isEqualTo(EMAIL);
    assertThat(account.role).isEqualTo("CUSTOMER");
    assertThat(account.password).isNotEqualTo(password);
    assertThat(encoder.matches(password, account.password)).isTrue();

    when(accounts.findByEmail(EMAIL)).thenReturn(Optional.of(account));
    seed.run();
    verify(accounts, times(1)).save(any());
  }

  @Test
  void rejectsUnsafeProfilesAndPersistentDatabaseBeforeReadingAccounts() {
    environment.setActiveProfiles("portfolio-demo", "prod");
    assertThatIllegalStateException().isThrownBy(seed::run);
    environment.setActiveProfiles("portfolio-demo", "mysql");
    assertThatIllegalStateException().isThrownBy(seed::run);
    environment.setActiveProfiles("portfolio-demo");
    environment.setProperty("spring.datasource.url", "jdbc:h2:file:./data/persistent");
    assertThatIllegalStateException().isThrownBy(seed::run);
    environment.setActiveProfiles("demo");
    assertThatIllegalStateException().isThrownBy(seed::run);
    verifyNoInteractions(accounts);
  }

  @Test
  void requiresCredentialsFromEnvironmentRatherThanSpringProperties() {
    environment.setProperty("RUNWAY_PORTFOLIO_DEMO_EMAIL", EMAIL);
    environment.setProperty("RUNWAY_PORTFOLIO_DEMO_PASSWORD", password);
    doReturn(Map.of()).when(environment).getSystemEnvironment();
    assertThatIllegalStateException().isThrownBy(seed::run);
    verifyNoInteractions(accounts);
  }

  @Test
  void rejectsCollisionWithDifferentCredentialsOrRole() {
    var account = new Account();
    account.password = encoder.encode(UUID.randomUUID().toString());
    when(accounts.findByEmail(EMAIL)).thenReturn(Optional.of(account));
    assertThatIllegalStateException().isThrownBy(seed::run);
    account.role = "ADMIN";
    account.password = encoder.encode(password);
    assertThatIllegalStateException().isThrownBy(seed::run);
    verify(accounts, never()).save(any());
  }
}
