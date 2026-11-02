package vn.runway.store.service;

import vn.runway.store.web.ApiErrors.BadRequest;

public final class PageBounds {
  private PageBounds() {}

  public static void check(int p, int s) {
    if (p < 0 || p > 100000 || s < 1 || s > 60)
      throw new BadRequest("Phân trang không hợp lệ: size 1–60.");
  }
}
