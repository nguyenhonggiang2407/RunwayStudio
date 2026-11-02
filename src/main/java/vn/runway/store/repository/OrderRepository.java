package vn.runway.store.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.runway.store.model.ShopOrder;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
  List<ShopOrder> findByAccountIdOrderByCreatedAtDesc(Long accountId);

  org.springframework.data.domain.Page<ShopOrder> findByAccountId(
      Long accountId, org.springframework.data.domain.Pageable page);

  long countByStatus(String status);

  @Query("select coalesce(sum(o.total),0) from ShopOrder o where o.status='DELIVERED'")
  long deliveredRevenue();

  @Query(
      "select function('date',h.createdAt),count(o.id),sum(o.total) from ShopOrder o join"
          + " OrderStatusHistory h on h.orderId=o.id where h.status='DELIVERED' and"
          + " h.createdAt>=:from and h.createdAt<:to group by function('date',h.createdAt) order by"
          + " function('date',h.createdAt)")
  List<Object[]> sales(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

  Optional<ShopOrder> findByRequestKey(String requestKey);

  List<ShopOrder> findAllByOrderByCreatedAtDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from ShopOrder o where o.id=:id")
  Optional<ShopOrder> lockById(@Param("id") Long id);
}
