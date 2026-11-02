package vn.runway.store.service;

import java.time.Instant;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
public class PromotionService {
  public record Discount(PromotionCode promotion, long amount) {}

  private final PromotionCodeRepository codes;
  private final PromotionRedemptionRepository redemptions;
  private final AuditService audit;
  private final CurrentAccount current;

  public PromotionService(
      PromotionCodeRepository c,
      PromotionRedemptionRepository r,
      AuditService a,
      CurrentAccount u) {
    codes = c;
    redemptions = r;
    audit = a;
    current = u;
  }

  public static long shipping(long subtotal) {
    return subtotal == 0 || subtotal >= 2000000 ? 0 : 30000;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Discount check(String code, long subtotal, Long buyer, boolean lock) {
    if (code == null || code.isBlank()) return new Discount(null, 0);
    String key = code.strip().toUpperCase(Locale.ROOT);
    var p =
        (lock ? codes.lockByCode(key) : codes.findByCode(key))
            .orElseThrow(() -> new BadRequest("Mã ưu đãi không hợp lệ."));
    if (!p.active
        || !p.expiresAt.isAfter(Instant.now())
        || p.used >= p.usageLimit
        || subtotal < p.minSpend
        || redemptions.existsByPromotionIdAndAccountId(p.id, buyer))
      throw new BadRequest("Ưu đãi đã hết hạn, đã dùng hoặc chưa đủ điều kiện.");
    return new Discount(p, Math.min(p.maxDiscount, subtotal * p.percentOff / 100));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void redeem(Discount d, Long account, Long order) {
    if (d.promotion() == null) return;
    d.promotion().used++;
    var r = new PromotionRedemption();
    r.promotionId = d.promotion().id;
    r.accountId = account;
    r.orderId = order;
    redemptions.save(r);
  }

  private Responses.Promotion dto(PromotionCode p) {
    return new Responses.Promotion(
        p.id,
        p.code,
        p.percentOff,
        p.minSpend,
        p.maxDiscount,
        p.usageLimit,
        p.used,
        p.expiresAt,
        p.active);
  }

  @Transactional(readOnly = true)
  public Responses.Page<Responses.Promotion> list(int page, int size) {
    PageBounds.check(page, size);
    return Responses.Page.of(codes.findAll(PageRequest.of(page, size)).map(this::dto));
  }

  @Transactional
  public Responses.Promotion create(Requests.Promotion i) {
    if (!i.expiresAt().isAfter(Instant.now()))
      throw new BadRequest("Ngày hết hạn phải ở tương lai.");
    var p = new PromotionCode();
    p.code = i.code();
    p.percentOff = i.percentOff();
    p.minSpend = i.minSpend();
    p.maxDiscount = i.maxDiscount();
    p.usageLimit = i.usageLimit();
    p.expiresAt = i.expiresAt();
    p.active = i.active();
    codes.saveAndFlush(p);
    audit.record(current.require().id, "CREATE", "PROMOTION", p.id);
    return dto(p);
  }
}
