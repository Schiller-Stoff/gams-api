package org.zim.gamsapi.DigitalObject;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.MultiValueMap;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Specification for filtering Digital Objects based on Dublin Core entry criteria.
 * This specification allows complex querying of digital objects through their associated
 * Dublin Core metadata using JPA Criteria API.
 *
 * Key benefits:
 * - Type-safe queries
 * - Complex boolean logic support
 * - Performance optimized with EXISTS subqueries
 * - Reusable and composable specifications
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

  /**
   * Convenience constructor for exact match searches
   */
  public DigitalObjectDublinCoreSpecification(
      MultiValueMap<String, String> dublinCoreFilters,
      Set<String> projectAbbreviations) {
    this(dublinCoreFilters, projectAbbreviations, SearchMode.EXACT_MATCH);
  }

  @Override
  public Predicate toPredicate(Root<DigitalObject> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
    List<Predicate> predicates = new ArrayList<>();

    // Add project filter if specified
    if (projectAbbreviations != null && !projectAbbreviations.isEmpty()) {
      predicates.add(root.get("project").get("projectAbbr").in(projectAbbreviations));
    }

    // Add Dublin Core filters if specified
    if (dublinCoreFilters != null && !dublinCoreFilters.isEmpty()) {
      List<Predicate> dcPredicates = createDublinCorePredicates(root, query, cb);
      if (!dcPredicates.isEmpty()) {
        // Combine all DC predicates with OR (digital object matches if ANY DC criteria is met)
        predicates.add(cb.or(dcPredicates.toArray(new Predicate[0])));
      }
    }

    // Ensure DISTINCT results for queries that might produce duplicates
    if (query != null) {
      query.distinct(true);
    }

    return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
  }

  /**
   * Creates Dublin Core predicates using EXISTS subqueries for optimal performance.
   * This approach avoids expensive JOINs and handles one-to-many relationships efficiently.
   */
  private List<Predicate> createDublinCorePredicates(Root<DigitalObject> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
    List<Predicate> dcPredicates = new ArrayList<>();

    dublinCoreFilters.forEach((dcName, values) -> {
      if (values != null && !values.isEmpty()) {
        for (String value : values) {
          if (value != null && !value.trim().isEmpty()) {
            Predicate dcPredicate = createDublinCoreExistsPredicate(root, cb, dcName, value.trim());
            if (dcPredicate != null) {
              dcPredicates.add(dcPredicate);
            }
          }
        }
      }
    });

    return dcPredicates;
  }

  /**
   * Creates an EXISTS subquery predicate for Dublin Core criteria.
   * This is more efficient than JOIN queries for one-to-many relationships.
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
        // For fulltext mode, we ignore the dcName and search across all DC fields
        return cb.like(
            cb.lower(valueExpression),
            cb.literal("%" + dcValue.toLowerCase() + "%")
        );

      default:
        log.warn("Unknown search mode: {}, defaulting to EXACT_MATCH", searchMode);
        return cb.equal(valueExpression, dcValue);
    }
  }

  /**
   * Static factory methods for common use cases
   */

  public static DigitalObjectDublinCoreSpecification exactMatch(
      MultiValueMap<String, String> filters, Set<String> projects) {
    return new DigitalObjectDublinCoreSpecification(filters, projects, SearchMode.EXACT_MATCH);
  }

  public static DigitalObjectDublinCoreSpecification contains(
      MultiValueMap<String, String> filters, Set<String> projects) {
    return new DigitalObjectDublinCoreSpecification(filters, projects, SearchMode.CONTAINS);
  }

  public static DigitalObjectDublinCoreSpecification fulltext(
      MultiValueMap<String, String> filters, Set<String> projects) {
    return new DigitalObjectDublinCoreSpecification(filters, projects, SearchMode.FULLTEXT);
  }

  /**
   * Combines this specification with another using AND logic
   */
  public Specification<DigitalObject> and(Specification<DigitalObject> other) {
    return Specification.where(this).and(other);
  }

  /**
   * Combines this specification with another using OR logic
   */
  public Specification<DigitalObject> or(Specification<DigitalObject> other) {
    return Specification.where(this).or(other);
  }

  /**
   * Creates a specification for project filtering only
   */
  public static Specification<DigitalObject> byProjects(Set<String> projectAbbreviations) {
    return (root, query, cb) -> {
      if (projectAbbreviations == null || projectAbbreviations.isEmpty()) {
        return cb.conjunction();
      }
      return root.get("project").get("projectAbbr").in(projectAbbreviations);
    };
  }

  /**
   * Creates a specification for object type filtering
   */
  public static Specification<DigitalObject> byObjectType(String objectType) {
    return (root, query, cb) -> {
      if (objectType == null || objectType.trim().isEmpty()) {
        return cb.conjunction();
      }
      return cb.equal(root.get("objectType"), objectType.trim());
    };
  }

  /**
   * Creates a specification for date range filtering
   */
  public static Specification<DigitalObject> byDateRange(java.util.Date from, java.util.Date to) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("created"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("created"), to));
      }
      return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}