package vn.runway.store.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import vn.runway.store.repository.AccountRepository;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(AccountRepository accounts) {
    return email ->
        accounts
            .findByEmail(email.toLowerCase(java.util.Locale.ROOT))
            .map(a -> User.withUsername(a.email).password(a.password).roles(a.role).build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid login"));
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
    return c.getAuthenticationManager();
  }

  @Bean
  SecurityContextRepository contextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      SecurityContextRepository context,
      com.fasterxml.jackson.databind.ObjectMapper json)
      throws Exception {
    http.csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .securityContext(c -> c.securityContextRepository(context))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.GET, "/api/me")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/orders/**", "/api/me/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/checkout", "/api/checkout/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (r, s, x) -> {
                          s.setStatus(401);
                          s.setContentType("application/json;charset=UTF-8");
                          json.writeValue(
                              s.getWriter(),
                              vn.runway.store.web.ApiErrors.body(
                                  "AUTHENTICATION_REQUIRED", "Vui lòng đăng nhập.", r));
                        })
                    .accessDeniedHandler(
                        (r, s, x) -> {
                          s.setStatus(403);
                          s.setContentType("application/json;charset=UTF-8");
                          json.writeValue(
                              s.getWriter(),
                              vn.runway.store.web.ApiErrors.body(
                                  "ACCESS_DENIED",
                                  "Bạn không có quyền truy cập hoặc yêu cầu CSRF không hợp lệ.",
                                  r));
                        }))
        .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((r, s, a) -> s.setStatus(204)))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'"
                                + " 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:;"
                                + " connect-src 'self'; frame-ancestors 'none'; object-src 'none';"
                                + " base-uri 'self'")));
    return http.build();
  }
}
