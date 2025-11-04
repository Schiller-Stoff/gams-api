package org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.MultiValueMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification for filtering Dublin Core entries based on multiple criteria.
 * This specification allows filtering by multiple Dublin Core names and their corresponding values.
 *
 * @param <T> the type of the entity to filter
 */
@Slf4j
public class DCGenericSpecification<T> implements Specification<T> {

  private final MultiValueMap<String, String> combinedFilters;

  public DCGenericSpecification(MultiValueMap<String, String> combinedFilters) {
    this.combinedFilters = combinedFilters;
  }
  /**
   * Creates a predicate based on the provided filters.
   * The predicate will match entries where the name matches any of the keys in combinedFilters
   * and the value matches any of the corresponding values.
   *
   * @param root            the root of the entity to filter
   * @param query           the criteria query
   * @param criteriaBuilder the criteria builder to create predicates
   * @return a predicate that matches the specified criteria, or null if no filters are provided
   */
  @Override
  public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = new ArrayList<>();

    if (combinedFilters != null && !combinedFilters.isEmpty()) {
      List<Predicate> orPredicates = new ArrayList<>();

      combinedFilters.forEach((dcName, values) -> {
        if (values != null && !values.isEmpty()) {
          for (String value : values) {
            if (value != null && !value.isEmpty()) {
              // Create a predicate that checks both name and value match
              // TODO remove hardcoded dc field names
              Predicate namePredicate = criteriaBuilder.equal(root.get("name"), dcName);
              Predicate valuePredicate = criteriaBuilder.like(
                  // TODO remove hardcoded dc field names
                  root.get("value"),
                  value
              );

              // Combine name and value predicates with AND
              orPredicates.add(criteriaBuilder.and(namePredicate, valuePredicate));
            }
          }
        }
      });

      // Combine all name-value pairs with OR (we want entries matching any of the criteria)
      if (!orPredicates.isEmpty()) {
        predicates.add(criteriaBuilder.or(orPredicates.toArray(new Predicate[0])));
      }
    }

    // Return the final combined predicate or null if no predicates were created
    return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
