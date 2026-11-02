package vn.runway.store.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.runway.store.model.AuditEvent;
import vn.runway.store.repository.AuditEventRepository;

@Service
public class AuditService {
  private final AuditEventRepository events;

  public AuditService(AuditEventRepository r) {
    events = r;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Long actor, String action, String type, Long id) {
    var e = new AuditEvent();
    e.actorId = actor;
    e.action = action;
    e.targetType = type;
    e.targetId = id;
    events.save(e);
  }
}
