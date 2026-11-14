package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money_;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Product_;
import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.domain.Tag_;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Path;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ProductReportRepositoryImpl implements ProductReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ProductReportRow> findProductReport(
            String q, Category category, BigDecimal maxPrice, Integer minStock, String tag, String sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductReportRow> query = cb.createQuery(ProductReportRow.class);
        Root<Product> product = query.from(Product.class);
        Path<BigDecimal> amount = product.get(Product_.price).get(Money_.amount);

        List<Predicate> predicates = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(product.get(Product_.name)), pattern),
                    cb.like(cb.lower(product.get(Product_.sku)), pattern)));
        }
        if (category != null) {
            predicates.add(cb.equal(product.get(Product_.category), category));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(amount, maxPrice));
        }
        if (minStock != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get(Product_.stock), minStock));
        }
        if (tag != null && !tag.isBlank()) {
            Join<Product, Tag> tagJoin = product.join(Product_.tags, JoinType.INNER);
            predicates.add(cb.equal(cb.lower(tagJoin.get(Tag_.name)), tag.trim().toLowerCase(Locale.ROOT)));
        }

        Subquery<Long> tagCount = query.subquery(Long.class);
        Root<Tag> countedTag = tagCount.from(Tag.class);
        tagCount.select(cb.count(countedTag));
        tagCount.where(cb.isMember(product, countedTag.get(Tag_.products)));

        query.select(cb.construct(
                ProductReportRow.class,
                product.get(Product_.sku),
                product.get(Product_.name),
                amount,
                product.get(Product_.stock),
                tagCount));
        query.where(cb.and(predicates.toArray(Predicate[]::new)));
        query.groupBy(
                product.get(Product_.id),
                product.get(Product_.sku),
                product.get(Product_.name),
                amount,
                product.get(Product_.stock));
        applySort(cb, query, product, amount, tagCount, sort);
        return entityManager.createQuery(query).getResultList();
    }

    private static void applySort(
            CriteriaBuilder cb,
            CriteriaQuery<ProductReportRow> query,
            Root<Product> product,
            jakarta.persistence.criteria.Expression<BigDecimal> amount,
            jakarta.persistence.criteria.Expression<Long> tagCount,
            String requestedSort) {
        String[] parts = requestedSort == null || requestedSort.isBlank()
                ? new String[] {"name", "asc"}
                : requestedSort.split(",", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("sort must be <field>[,asc|desc]");
        }
        String field = parts[0].trim();
        String direction = parts.length == 1 ? "asc" : parts[1].trim().toLowerCase(Locale.ROOT);
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new IllegalArgumentException("sort direction must be asc or desc");
        }

        jakarta.persistence.criteria.Expression<?> expression = switch (field) {
            case "sku" -> product.get(Product_.sku);
            case "name" -> product.get(Product_.name);
            case "amount" -> amount;
            case "stock" -> product.get(Product_.stock);
            case "tagCount" -> tagCount;
            default -> throw new IllegalArgumentException("Unsupported product report sort field: " + field);
        };
        jakarta.persistence.criteria.Order order = direction.equals("desc")
                ? cb.desc(expression)
                : cb.asc(expression);
        query.orderBy(List.of(order));
    }
}
