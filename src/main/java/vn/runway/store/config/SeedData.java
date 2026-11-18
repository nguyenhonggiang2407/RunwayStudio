package vn.runway.store.config;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;

@Component
public class SeedData implements CommandLineRunner {
  private final org.springframework.core.env.Environment environment;
  private final StudioDemoSeed studio;
  private final ProductRepository products;
  private final VariantRepository variants;
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;
  private final ObjectMapper json;

  @Value("${app.seed.enabled:false}")
  boolean enabled;

  @Value("${app.seed.admin-password}")
  String adminPassword;

  @Value("${app.seed.customer-password}")
  String customerPassword;

  public SeedData(
      ProductRepository p,
      VariantRepository v,
      AccountRepository a,
      PasswordEncoder e,
      ObjectMapper j,
      org.springframework.core.env.Environment env,
      StudioDemoSeed studio) {
    environment = env;
    this.studio = studio;
    products = p;
    variants = v;
    accounts = a;
    encoder = e;
    json = j;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    if (!enabled) return;
    if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")))
      throw new IllegalStateException("Demo seed is forbidden in prod");
    if (products.count() == 0) {
      try (var stream = getClass().getResourceAsStream("/catalog-seed.json")) {
        for (JsonNode n : json.readTree(stream)) {
          Product p = new Product();
          p.id = n.path("id").asLong();
          p.slug = n.path("slug").asText();
          p.name = n.path("name").asText();
          p.category = n.path("category").asText();
          p.description = n.path("description").asText();
          p.image = n.path("image").asText();
          products.save(p);
          for (JsonNode x : n.path("variants")) {
            Variant v = new Variant();
            v.id = x.path("id").asLong();
            v.productId = p.id;
            v.size = x.path("size").asText();
            v.price = x.path("price").asLong();
            v.stock = x.path("stock").asInt();
            variants.save(v);
          }
        }
      }
    }
    if (!environment.matchesProfiles("portfolio-demo")) {
      seedUser("admin@runway.local", "Quản trị Runway", adminPassword, "ADMIN");
      seedUser("demo@runway.local", "Khách hàng demo", customerPassword, "CUSTOMER");
    }
    studio.seed();
  }

  private void seedUser(String email, String name, String password, String role) {
    if (password.isEmpty()) return;
    if (password.isBlank()
        || password.length() < 10
        || password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new IllegalStateException("Demo passwords require 10 characters and at most 72 UTF-8 bytes.");
    }
    if (accounts.findByEmail(email).isEmpty()) {
      Account a = new Account();
      a.email = email;
      a.name = name;
      a.password = encoder.encode(password);
      a.role = role;
      accounts.save(a);
    }
  }
}
