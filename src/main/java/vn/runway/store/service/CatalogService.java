package vn.runway.store.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.Responses;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final ProductRepository products;
  private final VariantRepository variants;
  private final EntityManager em;

  public CatalogService(ProductRepository p, VariantRepository v, EntityManager e) {
    products = p;
    variants = v;
    em = e;
  }

  public ProductRepository products() {
    return products;
  }

  public VariantRepository variants() {
    return variants;
  }

  public Responses.Variant variant(Variant v) {
    return new Responses.Variant(v.id, v.productId, v.size, v.price, v.stock, v.version);
  }

  private Responses.Product map(Product p, List<Variant> vs) {
    return new Responses.Product(
        p.id,
        p.slug,
        p.name,
        p.category,
        Objects.toString(p.description, ""),
        Objects.toString(p.image, ""),
        p.active,
        vs.stream().map(this::variant).toList(),
        vs.stream().mapToLong(v -> v.price).min().orElse(0));
  }

  public Map<Long, Responses.Product> summaries(Collection<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    var vs =
        variants.findByProductIdInOrderByIdAsc(ids).stream()
            .collect(Collectors.groupingBy(v -> v.productId));
    return products.findAllById(ids).stream()
        .collect(Collectors.toMap(p -> p.id, p -> map(p, vs.getOrDefault(p.id, List.of()))));
  }

  public Responses.Product product(Product p) {
    return map(p, variants.findByProductIdOrderByIdAsc(p.id));
  }

  public Responses.Product bySlug(String slug) {
    var p = products.findBySlug(slug).orElseThrow(() -> new Missing("Không tìm thấy sản phẩm."));
    if (!p.active) throw new Missing("Sản phẩm đã ngừng bán.");
    return product(p);
  }

  public Product get(Long id) {
    return products.findById(id).orElseThrow(() -> new Missing("Không tìm thấy sản phẩm."));
  }

  public List<String> categories() {
    return products.categories();
  }

  private <T> List<Predicate> predicates(
      CriteriaBuilder cb,
      CriteriaQuery<T> query,
      Root<Product> root,
      String q,
      String category,
      boolean all,
      Long min,
      Long max,
      boolean inStock) {
    var terms = new ArrayList<Predicate>();
    if (!all) terms.add(cb.isTrue(root.get("active")));
    if (q != null && !q.isBlank()) {
      String pattern =
          "%"
              + q.strip()
                  .toLowerCase(Locale.ROOT)
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_")
              + "%";
      terms.add(
          cb.or(
              cb.like(cb.lower(root.get("name")), pattern, '\\'),
              cb.like(cb.lower(root.get("category")), pattern, '\\')));
    }
    if (category != null && !category.isBlank())
      terms.add(cb.equal(root.get("category"), category));
    var exists = query.subquery(Long.class);
    var v = exists.from(Variant.class);
    var vt = new ArrayList<Predicate>();
    vt.add(cb.equal(v.get("productId"), root.get("id")));
    if (min != null) vt.add(cb.ge(v.get("price"), min));
    if (max != null) vt.add(cb.le(v.get("price"), max));
    if (inStock) vt.add(cb.gt(v.get("stock"), 0));
    exists.select(v.get("id")).where(vt.toArray(Predicate[]::new));
    if (!all || min != null || max != null || inStock) terms.add(cb.exists(exists));
    return terms;
  }

  public Responses.Page<Responses.Product> page(
      String q,
      String category,
      int page,
      int size,
      String sort,
      Long min,
      Long max,
      boolean inStock,
      boolean all) {
    PageBounds.check(page, size);
    if (q != null && q.length() > 150
        || min != null && min < 0
        || max != null && max < 0
        || min != null && max != null && min > max
        || !Set.of("default", "price-asc", "price-desc", "name").contains(sort))
      throw new BadRequest("Bộ lọc không hợp lệ.");
    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(Product.class);
    var root = query.from(Product.class);
    query
        .select(root)
        .where(
            predicates(cb, query, root, q, category, all, min, max, inStock)
                .toArray(Predicate[]::new));
    if (sort.startsWith("price")) {
      var price = query.subquery(Long.class);
      var v = price.from(Variant.class);
      price
          .select(cb.min(v.<Long>get("price")))
          .where(cb.equal(v.get("productId"), root.get("id")));
      query.orderBy(sort.endsWith("asc") ? cb.asc(price) : cb.desc(price), cb.asc(root.get("id")));
    } else
      query.orderBy(cb.asc(root.get(sort.equals("name") ? "name" : "id")), cb.asc(root.get("id")));
    var rows =
        em.createQuery(query).setFirstResult(page * size).setMaxResults(size).getResultList();
    var count = cb.createQuery(Long.class);
    var cr = count.from(Product.class);
    count
        .select(cb.count(cr))
        .where(
            predicates(cb, count, cr, q, category, all, min, max, inStock)
                .toArray(Predicate[]::new));
    long total = em.createQuery(count).getSingleResult();
    var vs =
        rows.isEmpty()
            ? Map.<Long, List<Variant>>of()
            : variants.findByProductIdInOrderByIdAsc(rows.stream().map(p -> p.id).toList()).stream()
                .collect(Collectors.groupingBy(v -> v.productId));
    return new Responses.Page<>(
        rows.stream().map(p -> map(p, vs.getOrDefault(p.id, List.of()))).toList(),
        page,
        size,
        total,
        (int) ((total + size - 1) / size));
  }

  public List<Responses.Product> list(String q, String category, boolean all) {
    return page(q, category, 0, 60, "default", null, null, false, all).items();
  }
}
