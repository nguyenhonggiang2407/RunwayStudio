import { test, expect } from "@playwright/test";
import { mkdir } from "node:fs/promises";
const fixturePassword = "E2eFixture@12345";
async function send(request, url, data, method = "post") {
  const t = await (await request.get("/api/csrf")).json();
  return request[method](url, { data, headers: { [t.headerName]: t.token } });
}
async function login(page, request, admin = false) {
  const email = admin
    ? "admin@runway.local"
    : `e2e-${Date.now()}-${Math.random().toString(36).slice(2)}@example.test`;
  if (!admin)
    expect(
      (
        await send(request, "/api/register", {
          name: "E2E Fixture",
          email,
          password: fixturePassword,
        })
      ).status(),
    ).toBe(200);
  await page.goto("/account");
  const f = page.locator("#login-form");
  await f.getByLabel("Email", { exact: true }).fill(email);
  await f
    .getByLabel("Mật khẩu", { exact: true })
    .fill(admin ? "Admin@12345" : fixturePassword);
  await f.getByRole("button", { name: "Đăng nhập ↗", exact: true }).click();
  await expect(page.getByRole("heading", { name: /Xin chào/ })).toBeVisible();
}
async function noOverflow(page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBe(true);
}
async function shot(page, name, info) {
  await mkdir("docs/screenshots", { recursive: true });
  await page.evaluate(() => scrollTo({top:0,left:0,behavior:"instant"}));
  await page.screenshot({
    path: `docs/screenshots/${name}-${info.project.name}.png`,
    fullPage: false,
  });
}
let errors = [];
test.beforeEach(async ({ page }) => {
  errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
});
test.afterEach(() => expect(errors).toEqual([]));
test("guest catalog, variant, cart and authentication boundary", async ({
  page,
}, info) => {
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "YOUR STYLE. YOUR STUDIO." }),
  ).toBeVisible();
  await noOverflow(page);
  await shot(page, "home", info);
  await page
    .getByRole("link", { name: "Cửa hàng", exact: true })
    .first()
    .click();
  await page.getByRole("searchbox", { name: "Tìm sản phẩm" }).fill("Straight");
  await page.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await expect(page.locator("#product-grid .product-card")).toHaveCount(1);
  await page.locator("#product-grid").getByRole("link").first().click();
  await page.getByRole("button", { name: "M", exact: true }).click();
  await page.getByRole("button", { name: /Thêm vào giỏ hàng/ }).click();
  await expect(page.locator("#cart-count")).toHaveText("1");
  await page.getByRole("link", { name: /^Giỏ / }).click();
  await expect(page.locator(".cart-line")).toHaveCount(1);
  await expect(page.locator(".cart-line")).toContainText("M");
  await page.getByRole("link", { name: /Tiến hành đặt hàng/ }).click();
  await expect(
    page.getByRole("heading", { name: "Đăng nhập để đặt hàng." }),
  ).toBeVisible();
  await noOverflow(page);
});
test("save reload outfit, add-all, voucher checkout and timeline", async ({
  page,
  request,
}, info) => {
  await login(page, request);
  await page.goto("/studio");
  await page
    .getByRole("button", { name: "Phối theo ý bạn ↗", exact: true })
    .click();
  await expect(page.locator(".board-top")).toContainText("03 OBJECTS");
  await page.locator("#variant-TOP").selectOption({ label: "M · 4.975.000 ₫" });
  await page
    .getByLabel("Tên bộ phối", { exact: true })
    .fill("E2E everyday edit");
  await page.getByRole("button", { name: "Lưu bộ phối", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "E2E everyday edit", exact: true }),
  ).toBeVisible();
  await noOverflow(page);
  await shot(page, "outfit", info);
  await page.reload();
  await page
    .getByRole("button", { name: "Mở bộ phối ↗", exact: true })
    .click();
  await expect(page.locator("#variant-TOP option:checked")).toContainText("M");
  await page
    .getByRole("button", { name: "Thêm cả bộ vào giỏ ↗", exact: true })
    .click();
  await expect(page.locator("#cart-count")).toHaveText("3");
  await page.goto("/cart");
  await expect(page.locator(".cart-line")).toHaveCount(3);
  await page.getByRole("link", { name: /Tiến hành đặt hàng/ }).click();
  await page.getByLabel("Số điện thoại", { exact: true }).fill("0912345678");
  await page
    .getByLabel("Địa chỉ nhận hàng", { exact: true })
    .fill("12 Test Street, Hanoi");
  await page.locator("[name=voucherCode]").fill("STUDIO10");
  await page
    .getByRole("button", { name: "Kiểm tra ưu đãi", exact: true })
    .click();
  await expect(page.locator("#quote-result")).toContainText("150.000");
  await page.getByRole("button", { name: /Xác nhận đặt hàng/ }).click();
  await expect(
    page.getByRole("heading", { name: "Đặt hàng thành công." }),
  ).toBeVisible();
  await page.getByRole("link", { name: /Theo dõi đơn hàng/ }).click();
  await expect(page.locator(".order-card")).toHaveCount(1);
  await expect(page.locator(".order-timeline")).toContainText("Chờ xác nhận");
  await noOverflow(page);
});
test("scene hotspot to cart and explained shoe size", async ({
  page,
}, info) => {
  await page.goto("/scenes/city");
  await page
    .getByRole("button", { name: "Xem Daily Sweat", exact: true })
    .click();
  const d = page.getByRole("dialog");
  await expect(d).toBeVisible();
  await expect(d.getByRole("heading")).toHaveText("Daily Sweat");
  await d.getByRole("button", { name: "Thêm vào giỏ ↗", exact: true }).click();
  await expect(d).not.toBeVisible();
  await expect(page.locator("#cart-count")).toHaveText("1");
  await noOverflow(page);
  await shot(page, "scene", info);
  await page.getByRole("link", { name: "Fit Finder", exact: true }).click();
  await page.locator("#fit-category").selectOption("Giày");
  await page
    .getByLabel("Chiều dài bàn chân (cm)", { exact: true })
    .fill("25.5");
  await page
    .getByRole("button", { name: "Đối chiếu bảng size ↗", exact: true })
    .click();
  await expect(page.locator("#fit-result h2")).toHaveText("41");
  await expect(page.locator("#fit-result")).toContainText(
    "Số đo không được lưu",
  );
  await noOverflow(page);
});
test("admin draft editor, keyboard coordinate and publish flow", async ({
  page,
  request,
}, info) => {
  await login(page, request, true);
  await page.goto("/admin?view=scenes");
  await expect(
    page.getByRole("heading", { name: "Scenes & hotspots." }),
  ).toBeVisible();
  const slug = "e2e-scene-" + Date.now();
  await page.getByLabel("Tiêu đề", { exact: true }).fill("E2E scene");
  await page.getByLabel("Slug", { exact: true }).fill(slug);
  await page
    .getByRole("button", { name: "+ Thêm hotspot", exact: true })
    .click();
  await page.locator("[name=x-0]").fill("23");
  await page.locator("[name=y-0]").fill("60");
  await page.locator("[name=label-0]").fill("Scene test object");
  await page.locator('[data-edit-hotspot="0"]').press("ArrowRight");
  await expect(page.locator("[name=x-0]")).toHaveValue("24.0");
  await page.getByRole("button", { name: "Lưu scene", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "Xuất bản scene", exact: true }),
  ).toBeVisible();
  expect((await request.get("/api/scenes/" + slug)).status()).toBe(404);
  await noOverflow(page);
  await shot(page, "admin-scene", info);
  await page
    .getByRole("button", { name: "Xuất bản scene", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Chuyển về bản nháp", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "Mở scene công khai ↗", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "E2E scene", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Xem Scene test object", exact: true }),
  ).toBeVisible();
  await noOverflow(page);
});
