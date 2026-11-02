package vn.runway.store.service;

import java.util.Optional;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.runway.store.dto.Responses;
import vn.runway.store.model.Account;
import vn.runway.store.repository.AccountRepository;

@Service
public class CurrentAccount {
  private final AccountRepository accounts;

  public CurrentAccount(AccountRepository a) {
    accounts = a;
  }

  public Optional<Account> optional() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken
        ? Optional.empty()
        : accounts.findByEmail(a.getName());
  }

  public Account require() {
    return optional()
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Vui lòng đăng nhập."));
  }

  public Responses.User safe(Account a) {
    return new Responses.User(a.id, a.name, a.email, a.role);
  }
}
