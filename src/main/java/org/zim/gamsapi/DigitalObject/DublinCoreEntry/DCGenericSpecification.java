package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

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
    // Prevent cartesian product issues in counting queries
    if (query.getResultType() == Long.class || query.getResultType() == long.class) {
      query.distinct(true);
    }

    // TODO seems weird
    if (combinedFilters == null || combinedFilters.isEmpty()) {
      return null;
    }

    List<Predicate> fieldPredicates = new ArrayList<>();

    // Process each filter field
    for (String dcName : combinedFilters.keySet()) {
      List<String> values = combinedFilters.get(dcName);

      if (values == null || values.isEmpty()) continue;

      // Create a list of value predicates for this field
      List<Predicate> valuePredicates = new ArrayList<>();
      for (String value : values) {
        if (value != null && !value.isEmpty()) {
          // For each value, we need entries where name=dcName AND value LIKE %value%
          // These individual conditions are combined with AND for the same field
          valuePredicates.add(
              criteriaBuilder.equal(
                  // TODO hardcoded dc field
                  root.get("value"),
                  value
              )
          );
        }
      }

      if (!valuePredicates.isEmpty()) {
        // Combine the name predicate with the AND of all value predicates
        // TODO hardcoded dc field
        Predicate namePredicate = criteriaBuilder.equal(root.get("name"), dcName);

        // Combine all values with AND for this field
        Predicate valuesAndPredicate = criteriaBuilder.and(valuePredicates.toArray(new Predicate[0]));

        // Add the combined field predicate to the list of field predicates
        fieldPredicates.add(criteriaBuilder.and(namePredicate, valuesAndPredicate));
      }
    }

    // Combine different field predicates with OR
    return fieldPredicates.isEmpty() ? null :
        criteriaBuilder.or(fieldPredicates.toArray(new Predicate[0]));
  }
}
