package vn.runway.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.runway.store.model.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
  List<OrderLine> findByOrderIdOrderByIdAsc(Long orderId);
}
