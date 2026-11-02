package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  java.util.List<CartItem> findByAccountIdOrderByVariantIdAsc(Long id);

  void deleteByAccountId(Long id);
}
