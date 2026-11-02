package vn.runway.store.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.runway.store.model.Account;
import vn.runway.store.repository.AccountRepository;

class AdminBootstrapTest {
  final AccountRepository accounts = mock(AccountRepository.class);
  final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  AdminBootstrap input() {
    var b = new AdminBootstrap(accounts, encoder);
    b.enabled = true;
    b.email = "INITIAL@EXAMPLE.TEST";
    b.password = "BootstrapFixture@12345";
    return b;
  }

  @Test
  void disabledDoesNotProvision() {
    var b = input();
    b.enabled = false;
    b.run();
    verifyNoInteractions(accounts);
  }

  @Test
  void explicitStrongCredentialsRequired() {
    var b = input();
    b.password = "short";
    assertThatThrownBy(b::run).isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(accounts);
  }

  @Test
  void cannotPromoteCustomer() {
    var a = new Account();
    a.role = "CUSTOMER";
    when(accounts.findByEmail("initial@example.test")).thenReturn(Optional.of(a));
    assertThatThrownBy(input()::run).isInstanceOf(IllegalStateException.class);
    verify(accounts, never()).save(any());
  }

  @Test
  void createsAdminWithHash() {
    when(accounts.findByEmail("initial@example.test")).thenReturn(Optional.empty());
    var b = input();
    b.run();
    var captor = org.mockito.ArgumentCaptor.forClass(Account.class);
    verify(accounts).save(captor.capture());
    var a = captor.getValue();
    assertThat(a.email).isEqualTo("initial@example.test");
    assertThat(a.role).isEqualTo("ADMIN");
    assertThat(a.password).isNotEqualTo(b.password);
    assertThat(encoder.matches(b.password, a.password)).isTrue();
  }

  @Test
  void existingAdminIsNotOverwritten() {
    var a = new Account();
    a.role = "ADMIN";
    when(accounts.findByEmail("initial@example.test")).thenReturn(Optional.of(a));
    input().run();
    verify(accounts, never()).save(any());
  }
}
