package vn.runway.store.service;

import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.model.Account;
import vn.runway.store.repository.AccountRepository;
import vn.runway.store.web.ApiErrors.BadRequest;

@Service
public class AuthService {
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;
  private final AuthenticationManager manager;
  private final SecurityContextRepository contexts;
  private final CurrentAccount current;

  public AuthService(
      AccountRepository a,
      PasswordEncoder e,
      AuthenticationManager m,
      SecurityContextRepository c,
      CurrentAccount u) {
    accounts = a;
    encoder = e;
    manager = m;
    contexts = c;
    current = u;
  }

  @Transactional
  public Responses.Message register(Requests.Register i) {
    String email = i.email().strip().toLowerCase(Locale.ROOT);
    if (accounts.findByEmail(email).isPresent()) throw new BadRequest("Email đã được sử dụng.");
    if (i.password().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new BadRequest("Mật khẩu tối đa 72 byte UTF-8.");
    var a = new Account();
    a.email = email;
    a.name = i.name().strip();
    a.password = encoder.encode(i.password());
    accounts.save(a);
    return new Responses.Message("Đăng ký thành công. Bạn có thể đăng nhập.");
  }

  public Responses.User login(Requests.Login i, HttpServletRequest r, HttpServletResponse s) {
    var auth =
        manager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                i.email().strip().toLowerCase(Locale.ROOT), i.password()));
    r.getSession();
    r.changeSessionId();
    var c = SecurityContextHolder.createEmptyContext();
    c.setAuthentication(auth);
    SecurityContextHolder.setContext(c);
    contexts.saveContext(c, r, s);
    return current.safe(current.require());
  }
}
