package vn.runway.store;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import vn.runway.store.config.LoginRateLimitFilter;

class SecurityRegressionTest {
  static class MutableClock extends Clock {
    Instant now = Instant.parse("2026-11-02T00:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }

  MockHttpServletResponse call(LoginRateLimitFilter f, String ip) throws Exception {
    var r = new MockHttpServletRequest("POST", "/api/login");
    r.setServletPath("/api/login");
    r.setRemoteAddr(ip);
    r.setAttribute("requestId", "test-id");
    var s = new MockHttpServletResponse();
    f.doFilter(r, s, (a, b) -> ((jakarta.servlet.http.HttpServletResponse) b).setStatus(204));
    return s;
  }

  @Test
  void rateLimitReturns429WithRetryAfter() throws Exception {
    var f = new LoginRateLimitFilter(2, new ObjectMapper(), Clock.systemUTC());
    assertThat(call(f, "192.0.2.1").getStatus()).isEqualTo(204);
    assertThat(call(f, "192.0.2.1").getStatus()).isEqualTo(204);
    var denied = call(f, "192.0.2.1");
    assertThat(denied.getStatus()).isEqualTo(429);
    assertThat(denied.getHeader("Retry-After")).isNotBlank();
    assertThat(denied.getContentAsString()).contains("RATE_LIMITED");
  }

  @Test
  void windowsExpireAndPeersAreSeparate() throws Exception {
    var c = new MutableClock();
    var f = new LoginRateLimitFilter(1, new ObjectMapper(), c);
    assertThat(call(f, "192.0.2.1").getStatus()).isEqualTo(204);
    assertThat(call(f, "192.0.2.2").getStatus()).isEqualTo(204);
    assertThat(call(f, "192.0.2.1").getStatus()).isEqualTo(429);
    c.now = c.now.plusSeconds(901);
    assertThat(call(f, "192.0.2.1").getStatus()).isEqualTo(204);
  }
}
