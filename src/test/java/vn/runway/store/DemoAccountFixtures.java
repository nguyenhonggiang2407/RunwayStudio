package vn.runway.store;

import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class DemoAccountFixtures {
  static final String CUSTOMER_FIXTURE_PASSWORD = UUID.randomUUID().toString();
  private static final String ADMIN_FIXTURE_PASSWORD = UUID.randomUUID().toString();

  @DynamicPropertySource
  static void demoAccountPasswords(DynamicPropertyRegistry properties) {
    properties.add("app.seed.admin-password", () -> ADMIN_FIXTURE_PASSWORD);
    properties.add("app.seed.customer-password", () -> CUSTOMER_FIXTURE_PASSWORD);
  }
}
