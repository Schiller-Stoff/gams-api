package org.ddh.gamsapi.domain.DigitalObject.Facet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FIXED Facet Count Repository with corrected PostgreSQL native SQL
 */
@Repository
@Slf4j
public class FacetCountRepository {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * FIXED: Get facet counts using corrected PostgreSQL query
   */
  public Map<String, List<FacetValue>> getFacetCounts(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Set<String> facetFields) {

    if (facetFields.isEmpty()) {
      return Collections.emptyMap();
    }

    log.debug("Getting facet counts for projects: {}, facetFields: {}, selectedFacets: {}",
        projectAbbrs, facetFields, selectedFacets);

    String sql = buildFixedFacetCountQuery(selectedFacets);

    Query query = entityManager.createNativeQuery(sql);
    setQueryParameters(query, projectAbbrs, selectedFacets, facetFields);

    @SuppressWarnings("unchecked")
    List<Object[]> results = query.getResultList();

    log.debug("Facet count query returned {} results", results.size());

    return parseFacetResults(results, selectedFacets);
  }

  /**
   * FIXED: Build the corrected PostgreSQL query for facet counting
   */
  private String buildFixedFacetCountQuery(MultiValueMap<String, String> selectedFacets) {
    StringBuilder sql = new StringBuilder();

    // Base query - FIXED column references
    sql.append("SELECT dce.name as facet_field, dce.value as facet_value, COUNT(DISTINCT dob.id) as count ");
    sql.append("FROM dublin_core_entry dce ");
    sql.append("JOIN digital_object dob ON dob.id = dce.digital_object_id "); // FIXED: renamed alias
    sql.append("WHERE dob.project_project_abbr = ANY(:projectAbbrs) ");
    sql.append("AND dce.name = ANY(:facetFields) ");

    // Add filters for selected facets using EXISTS subqueries
    int filterIndex = 0;
    for (Map.Entry<String, List<String>> entry : selectedFacets.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        sql.append("AND EXISTS (");
        sql.append("  SELECT 1 FROM dublin_core_entry dce").append(filterIndex).append(" ");
        sql.append("  WHERE dce").append(filterIndex).append(".digital_object_id = dob.id "); // FIXED: use dob
        sql.append("  AND dce").append(filterIndex).append(".name = :filterName").append(filterIndex).append(" ");
        sql.append("  AND dce").append(filterIndex).append(".value = ANY(:filterValues").append(filterIndex).append(") ");
        // REMOVED the problematic drill-down logic for now - we'll add it back correctly later
        sql.append(") ");
        filterIndex++;
      }
    }

    sql.append("GROUP BY dce.name, dce.value ");
    sql.append("ORDER BY dce.name, count DESC, dce.value");

    String finalSql = sql.toString();
    log.debug("Generated SQL: {}", finalSql);

    return finalSql;
  }

  /**
   * Get facet values for a single field with proper drill-down logic
   */
  private List<FacetValue> getFacetValuesForField(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> otherFilters,
      String facetField,
      List<String> selectedValuesForThisField) {

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT dce.value, COUNT(DISTINCT dob.id) as count ");
    sql.append("FROM dublin_core_entry dce ");
    sql.append("JOIN digital_object dob ON dob.id = dce.digital_object_id ");
    sql.append("WHERE dob.project_project_abbr = ANY(:projectAbbrs) ");
    sql.append("AND dce.name = :facetField ");

    // Add filters for other selected facets
    int filterIndex = 0;
    for (Map.Entry<String, List<String>> entry : otherFilters.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        sql.append("AND EXISTS (");
        sql.append("  SELECT 1 FROM dublin_core_entry filter_dce").append(filterIndex).append(" ");
        sql.append("  WHERE filter_dce").append(filterIndex).append(".digital_object_id = dob.id ");
        sql.append("  AND filter_dce").append(filterIndex).append(".name = :filterName").append(filterIndex).append(" ");
        sql.append("  AND filter_dce").append(filterIndex).append(".value = ANY(:filterValues").append(filterIndex).append(") ");
        sql.append(") ");
        filterIndex++;
      }
    }

    sql.append("GROUP BY dce.value ");
    sql.append("ORDER BY count DESC, dce.value");

    Query query = entityManager.createNativeQuery(sql.toString());
    query.setParameter("projectAbbrs", projectAbbrs.toArray(new String[0]));
    query.setParameter("facetField", facetField);

    // Set filter parameters
    filterIndex = 0;
    for (Map.Entry<String, List<String>> entry : otherFilters.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        query.setParameter("filterName" + filterIndex, entry.getKey());
        query.setParameter("filterValues" + filterIndex, entry.getValue().toArray(new String[0]));
        filterIndex++;
      }
    }

    @SuppressWarnings("unchecked")
    List<Object[]> results = query.getResultList();

    List<String> selectedValues = selectedValuesForThisField != null ? selectedValuesForThisField : Collections.emptyList();

    return results.stream()
        .map(row -> FacetValue.builder()
            .value((String) row[0])
            .label((String) row[0])
            .count(((Number) row[1]).longValue())
            .selected(selectedValues.contains((String) row[0]))
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Set parameters for the native query
   */
  private void setQueryParameters(Query query, Set<String> projectAbbrs,
                                  MultiValueMap<String, String> selectedFacets,
                                  Set<String> facetFields) {

    query.setParameter("projectAbbrs", projectAbbrs.toArray(new String[0]));
    query.setParameter("facetFields", facetFields.toArray(new String[0]));

    // Set filter parameters
    int filterIndex = 0;
    for (Map.Entry<String, List<String>> entry : selectedFacets.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        query.setParameter("filterName" + filterIndex, entry.getKey());
        query.setParameter("filterValues" + filterIndex, entry.getValue().toArray(new String[0]));
        filterIndex++;
      }
    }
  }

  /**
   * Parse native query results into structured facet data
   */
  private Map<String, List<FacetValue>> parseFacetResults(List<Object[]> results,
                                                          MultiValueMap<String, String> selectedFacets) {

    Map<String, List<FacetValue>> facetCounts = new LinkedHashMap<>();

    // Group results by facet field
    Map<String, List<Object[]>> groupedResults = results.stream()
        .collect(Collectors.groupingBy(row -> (String) row[0]));

    groupedResults.forEach((facetField, rows) -> {
      List<String> selectedValues = selectedFacets.getOrDefault(facetField, Collections.emptyList());

      List<FacetValue> facetValues = rows.stream()
          .map(row -> FacetValue.builder()
              .value((String) row[1])
              .label((String) row[1])
              .count(((Number) row[2]).longValue())
              .selected(selectedValues.contains((String) row[1]))
              .build())
          .collect(Collectors.toList());

      facetCounts.put(facetField, facetValues);
    });

    return facetCounts;
  }

  /**
   * Get total count for baseline metrics
   */
  public long getTotalObjectCount(Set<String> projectAbbrs) {
    String sql = "SELECT COUNT(*) FROM digital_object WHERE project_project_abbr = ANY(:projectAbbrs)";

    Query query = entityManager.createNativeQuery(sql);
    query.setParameter("projectAbbrs", projectAbbrs.toArray(new String[0]));

    return ((Number) query.getSingleResult()).longValue();
  }

}