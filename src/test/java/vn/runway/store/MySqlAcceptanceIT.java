package vn.runway.store;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import vn.runway.store.dto.Requests;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.service.*;
import vn.runway.store.web.ApiErrors.BadRequest;

@SpringBootTest(
    properties = {
      "app.seed.enabled=true",
      "app.seed.admin-password=TestAdmin@12345",
      "app.seed.customer-password=TestCustomer@12345"
    })
@ActiveProfiles("mysql")
class MySqlAcceptanceIT {
  @Autowired OrderService service;
  @Autowired CatalogService catalog;
  @Autowired AccountRepository accounts;
  @Autowired VariantRepository variants;
  @Autowired OrderRepository orders;
  @Autowired InventoryMovementRepository movements;
  @Autowired PasswordEncoder encoder;

  Long variant() {
    return variants.findByProductIdOrderByIdAsc(2026009L).get(0).id;
  }

  Account buyer() {
    var a = new Account();
    a.email = "race-" + UUID.randomUUID() + "@example.test";
    a.name = "Concurrency fixture";
    a.password = encoder.encode(UUID.randomUUID().toString());
    return accounts.saveAndFlush(a);
  }

  Requests.Checkout input(String key) {
    return new Requests.Checkout("Test buyer", "0912345678", "12 Test Street, Hanoi", "", key);
  }

  void stock(int n) {
    var v = variants.findById(variant()).orElseThrow();
    v.stock = n;
    variants.saveAndFlush(v);
  }

  @Test
  void mysqlFreshSchemaAndSeedValidate() {
    assertThat(variants.count()).isEqualTo(50);
    assertThat(variants.findAll().stream().map(v -> v.productId).distinct().count()).isEqualTo(11);
  }

  @Test
  void mysqlCatalogPriceQueryWorks() {
    var p = catalog.page(null, null, 0, 3, "price-asc", 0L, 1000000L, true, false);
    assertThat(p.totalElements()).isEqualTo(2);
    assertThat(p.items().get(0).price()).isEqualTo(490000);
  }

  @RepeatedTest(3)
  void tenBuyersCompeteForLastItem() throws Exception {
    stock(1);
    long before = orders.count(), ledger = movements.count();
    Long v = variant();
    var buyers = new ArrayList<Account>();
    for (int n = 0; n < 10; n++) buyers.add(buyer());
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(10);
    var tasks = new ArrayList<Future<Boolean>>();
    try {
      for (var buyer : buyers)
        tasks.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    service.checkout(buyer, Map.of(v, 1), input(UUID.randomUUID().toString()));
                    return true;
                  } catch (BadRequest expected) {
                    return false;
                  }
                }));
      gate.countDown();
      int success = 0;
      for (var f : tasks) if (f.get(30, TimeUnit.SECONDS)) success++;
      assertThat(success).isEqualTo(1);
      assertThat(orders.count()).isEqualTo(before + 1);
      assertThat(variants.findById(v).orElseThrow().stock).isZero();
      assertThat(movements.count()).isEqualTo(ledger + 1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void concurrentRetriesCreateOneOrder() throws Exception {
    stock(10);
    long before = orders.count(), ledger = movements.count();
    var buyer = buyer();
    String key = UUID.randomUUID().toString();
    Long v = variant();
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(10);
    var tasks = new ArrayList<Future<Long>>();
    try {
      for (int n = 0; n < 10; n++)
        tasks.add(
            pool.submit(
                () -> {
                  gate.await();
                  return service.checkout(buyer, Map.of(v, 1), input(key)).order().id;
                }));
      gate.countDown();
      Set<Long> ids = new HashSet<>();
      for (var f : tasks) ids.add(f.get(30, TimeUnit.SECONDS));
      assertThat(ids).hasSize(1);
      assertThat(orders.count()).isEqualTo(before + 1);
      assertThat(variants.findById(v).orElseThrow().stock).isEqualTo(9);
      assertThat(movements.count()).isEqualTo(ledger + 1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void concurrentCancellationRestocksOnce() throws Exception {
    stock(5);
    var buyer = buyer();
    var o =
        service.checkout(buyer, Map.of(variant(), 1), input(UUID.randomUUID().toString())).order();
    long ledger = movements.count();
    var pool = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    var tasks = new ArrayList<Future<Boolean>>();
    try {
      for (int n = 0; n < 2; n++)
        tasks.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    service.change(o.id, "CANCELLED", buyer, false);
                    return true;
                  } catch (BadRequest expected) {
                    return false;
                  }
                }));
      gate.countDown();
      int success = 0;
      for (var f : tasks) if (f.get(30, TimeUnit.SECONDS)) success++;
      assertThat(success).isEqualTo(1);
      assertThat(variants.findById(variant()).orElseThrow().stock).isEqualTo(5);
      assertThat(movements.count()).isEqualTo(ledger + 1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void explicitBaselinePreservesUnversionedData() {
    String url = Objects.requireNonNull(System.getenv("LEGACY_DB_URL"));
    var ds =
        new DriverManagerDataSource(
            url, System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
    var j = new JdbcTemplate(ds);
    new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/mysql/V1__source_baseline.sql"))
        .execute(ds);
    j.update(
        "insert into accounts(email,name,password,role)"
            + " values('legacy@example.test','Fixture','not-a-login-hash','CUSTOMER')");
    j.update(
        "insert into products values(991,'legacy','Original name','Áo','Original"
            + " description','/media/owned/daily-sweat.svg',true)");
    j.update("insert into variants values(992,991,'M',123000,7,0)");
    j.update(
        "insert into"
            + " shop_orders(account_id,request_key,recipient,phone,address,status,subtotal,shipping,total,created_at)"
            + " values(1,'legacy-order','Fixture','0912345678','12 Test"
            + " Street','PLACED',123000,30000,153000,UTC_TIMESTAMP(6))");
    j.update(
        "insert into order_lines(order_id,variant_id,product_name,size,quantity,unit_price)"
            + " values(1,992,'Original snapshot','M',1,123000)");
    var f =
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/mysql")
            .baselineVersion("1")
            .load();
    f.baseline();
    f.migrate();
    f.validate();
    assertThat(j.queryForObject("select stock from variants where id=992", Integer.class))
        .isEqualTo(7);
    assertThat(j.queryForObject("select product_name from order_lines", String.class))
        .isEqualTo("Original snapshot");
    assertThat(j.queryForObject("select count(*) from order_status_history", Long.class))
        .isEqualTo(1);
    assertThat(j.queryForObject("select discount from shop_orders", Long.class)).isZero();
    assertThatThrownBy(() -> j.update("insert into variants values(993,99999,'M',123000,5,0)"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }
}
