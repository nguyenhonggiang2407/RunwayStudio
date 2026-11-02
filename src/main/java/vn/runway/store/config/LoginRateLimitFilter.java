package vn.runway.store.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.runway.store.web.ApiErrors;

@Component
@Order(Integer.MIN_VALUE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {
  private record Window(long until, int count) {}

  private final Map<String, Window> windows = new HashMap<>();
  private final int maximum;
  private final ObjectMapper json;
  private final Clock clock;

  @org.springframework.beans.factory.annotation.Autowired
  public LoginRateLimitFilter(@Value("${app.login.max-attempts:20}") int max, ObjectMapper json) {
    this(max, json, Clock.systemUTC());
  }

  public LoginRateLimitFilter(int max, ObjectMapper json, Clock clock) {
    maximum = max;
    this.json = json;
    this.clock = clock;
  }

  private synchronized long retry(String ip) {
    long now = clock.millis();
    windows.entrySet().removeIf(e -> e.getValue().until <= now);
    var w = windows.get(ip);
    if (w == null) {
      if (windows.size() >= 4096) return 900;
      w = new Window(now + 900000, 0);
    }
    if (w.count >= maximum) return Math.max(1, (w.until - now + 999) / 1000);
    windows.put(ip, new Window(w.until, w.count + 1));
    return 0;
  }

  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    if (r.getMethod().equals("POST")
        && Set.of("/api/login", "/api/register").contains(r.getServletPath())) {
      long after = retry(r.getRemoteAddr());
      if (after > 0) {
        s.setStatus(429);
        s.setHeader("Retry-After", Long.toString(after));
        s.setContentType("application/json;charset=UTF-8");
        json.writeValue(
            s.getWriter(),
            ApiErrors.body(
                "RATE_LIMITED", "Thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.", r));
        return;
      }
    }
    c.doFilter(r, s);
  }
}
