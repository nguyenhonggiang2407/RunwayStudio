package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.CustomerAddress;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
  java.util.List<CustomerAddress> findByAccountIdOrderByIdDesc(Long id);

  long countByAccountId(Long id);
}
