package org.ddh.gamsapi.domain.DigitalObject;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.MultiValueMap;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CORRECT Faceted Search Dublin Core Specification
 * Logic (traditional faceted search):
 * - Values for same DC field: OR (coverage=Wien&coverage=Nürnberg → Wien OR Nürnberg)
 * - Different DC fields: AND (coverage=Wien&creator=1 → Wien AND creator=1)
 * Your example: ?coverage=Wien&coverage=Nürnberg&creator=1
 * Result: (coverage=Wien OR coverage=Nürnberg) AND creator=1
 */
@Slf4j
public class DigitalObjectDublinCoreSpecification implements Specification<DigitalObject> {

  private final MultiValueMap<String, String> dublinCoreFilters;
  private final Set<String> projectAbbreviations;
  private final SearchMode searchMode;

  public enum SearchMode {
    EXACT_MATCH,    // Exact value matching
    CONTAINS,       // Case-insensitive LIKE search
    FULLTEXT        // Fulltext search across all DC fields
  }

  public DigitalObjectDublinCoreSpecification(
      MultiValueMap<String, String> dublinCoreFilters,
      Set<String> projectAbbreviations,
      SearchMode searchMode) {
    this.dublinCoreFilters = dublinCoreFilters;
    this.projectAbbreviations = projectAbbreviations;
    this.searchMode = searchMode;
  }

  @Override
  public Predicate toPredicate(Root<DigitalObject> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
    List<Predicate> predicates = new ArrayList<>();

    // Add project filter
    if (projectAbbreviations != null && !projectAbbreviations.isEmpty()) {
      predicates.add(root.get("project").get("projectAbbr").in(projectAbbreviations));
    }

    // Add Dublin Core filters
    if (dublinCoreFilters != null && !dublinCoreFilters.isEmpty()) {
      List<Predicate> fieldPredicates = new ArrayList<>();

      dublinCoreFilters.forEach((dcName, values) -> {
        if (values != null && !values.isEmpty()) {
          List<Predicate> valuePredicates = new ArrayList<>();

          // Create EXISTS predicate for each value
          for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
              Predicate existsPredicate = createDublinCoreExistsPredicate(root, cb, dcName, value.trim());
              if (existsPredicate != null) {
                valuePredicates.add(existsPredicate);
              }
            }
          }

          // CORRECTED: Combine values for same field with OR (faceted search logic)
          if (!valuePredicates.isEmpty()) {
            fieldPredicates.add(cb.or(valuePredicates.toArray(new Predicate[0])));
          }
        }
      });

      // Combine different fields with AND
      if (!fieldPredicates.isEmpty()) {
        predicates.add(cb.and(fieldPredicates.toArray(new Predicate[0])));
      }
    }

    // Ensure DISTINCT results
    if (query != null) {
      query.distinct(true);
    }

    return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
  }

  /**
   * Creates an EXISTS subquery predicate for Dublin Core criteria.
   */
  private Predicate createDublinCoreExistsPredicate(Root<DigitalObject> root, CriteriaBuilder cb, String dcName, String dcValue) {
    Subquery<Long> subquery = cb.createQuery().subquery(Long.class);
    Root<DublinCoreEntry> dcRoot = subquery.from(DublinCoreEntry.class);

    subquery.select(cb.literal(1L));

    List<Predicate> subqueryPredicates = new ArrayList<>();

    // Join condition: Dublin Core entry belongs to the digital object
    subqueryPredicates.add(cb.equal(dcRoot.get("digitalObject"), root));

    // Dublin Core name filter
    subqueryPredicates.add(cb.equal(dcRoot.get("name"), dcName));

    // Dublin Core value filter based on search mode
    Predicate valuePredicate = createValuePredicate(cb, dcRoot, dcValue);
    if (valuePredicate != null) {
      subqueryPredicates.add(valuePredicate);
    }

    subquery.where(cb.and(subqueryPredicates.toArray(new Predicate[0])));

    return cb.exists(subquery);
  }

  /**
   * Creates the appropriate value predicate based on the search mode.
   */
  private Predicate createValuePredicate(CriteriaBuilder cb, Root<DublinCoreEntry> dcRoot, String dcValue) {
    Expression<String> valueExpression = dcRoot.get("value");

    switch (searchMode) {
      case EXACT_MATCH:
        return cb.equal(valueExpression, dcValue);

      case CONTAINS:
        return cb.like(
            cb.lower(valueExpression),
            cb.literal("%" + dcValue.toLowerCase() + "%")
        );

      case FULLTEXT:
        return cb.like(
            cb.lower(valueExpression),
            cb.literal("%" + dcValue.toLowerCase() + "%")
        );

      default:
        log.warn("Unknown search mode: {}, defaulting to EXACT_MATCH", searchMode);
        return cb.equal(valueExpression, dcValue);
    }
  }
}