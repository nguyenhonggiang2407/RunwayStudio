package vn.runway.store.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.InventoryMovementRepository;
import vn.runway.store.web.ApiErrors.BadRequest;

@Service
public class InventoryService {
  private final InventoryMovementRepository movements;

  public InventoryService(InventoryMovementRepository r) {
    movements = r;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void adjust(Variant v, int delta, String reason, String type, Long ref, Long actor) {
    int next = Math.addExact(v.stock, delta);
    if (next < 0 || next > 1000000) throw new BadRequest("Tồn kho không hợp lệ.");
    if (delta == 0) return;
    v.stock = next;
    var m = new InventoryMovement();
    m.variantId = v.id;
    m.delta = delta;
    m.resultingStock = next;
    m.reason = reason;
    m.refType = type;
    m.refId = ref;
    m.actorId = actor;
    movements.save(m);
  }
}
