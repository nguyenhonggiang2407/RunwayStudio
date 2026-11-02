package vn.runway.store.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.runway.store.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findBySlug(String slug);

  List<Product> findByActiveTrueOrderByIdAsc();

  @org.springframework.data.jpa.repository.Query(
      "select distinct p.category from Product p where p.active=true order by p.category")
  List<String> categories();
}
