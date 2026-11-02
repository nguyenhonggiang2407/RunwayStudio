package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
  java.util.List<OrderStatusHistory> findByOrderIdOrderByIdAsc(Long id);
}
