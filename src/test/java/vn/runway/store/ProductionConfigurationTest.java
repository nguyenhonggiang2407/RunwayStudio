package vn.runway.store;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.runway.store.repository.AccountRepository;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:prod-config;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password="
    })
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class ProductionConfigurationTest {
  @Autowired AccountRepository accounts;
  @Autowired Environment environment;
  @Autowired MockMvc mvc;
  @Autowired ApplicationContext context;

  @Test
  void prodNeverSeedsDemoAccounts() {
    assertThat(accounts.count()).isZero();
    assertThat(environment.getProperty("app.seed.enabled", Boolean.class)).isFalse();
  }

  @Test
  void secureCookiesAndValidatedSchema() {
    assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class))
        .isTrue();
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
  }

  @Test
  void inlineScriptBlockedAndSensitiveActuatorAbsent() throws Exception {
    mvc.perform(get("/"))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    org.hamcrest.Matchers.containsString("script-src 'self';")));
    mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
  }

  @Test
  void reviewedSpringAdvisoryPreconditionsRemainAbsent() {
    assertThat(
            context.getBeansOfType(
                org.springframework.web.servlet.view.xslt.XsltViewResolver.class))
        .isEmpty();
    var m =
        context.getBean(
            "requestMappingHandlerMapping",
            org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
                .class);
    for (var h : m.getHandlerMethods().values())
      if (h.getBeanType().getPackageName().startsWith("vn.runway.store"))
        assertThat(
                org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.class
                    .isAssignableFrom(h.getReturnType().getParameterType()))
            .isFalse();
  }
}
