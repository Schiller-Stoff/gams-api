package org.ddh.gamsapi.application.Integration.BaseSearch;

/**
 * Defines how Dublin Core field filters should match values.
 *
 */
public enum DublinCoreSearchMode {

  // TODO rework class name / location in package / etc.

  /**
   * Exact phrase matching using the original {@code strings} typed fields.
   *
   */
  PHRASE,

  /**
   * Tokenized substring matching using copyField {@code text_general} typed fields.
   *
   */
  SUBSTRING;

  /**
   * Returns the Solr field name to use for this search mode.
   *
   * @param dcFieldName The base Dublin Core field name (e.g., "dc.subject")
   * @return The Solr field name to query based on this search mode
   * @throws IllegalArgumentException if dcFieldName is null or empty
   */
  public String getSolrFieldName(String dcFieldName) {
    if (dcFieldName == null || dcFieldName.trim().isEmpty()) {
      throw new IllegalArgumentException("DC field name cannot be null or empty");
    }

    // TODO why is there normalization everywhere?
    String normalizedField = dcFieldName.startsWith("dc.")
        ? dcFieldName
        : "dc." + dcFieldName;

    return switch (this) {
      case PHRASE -> normalizedField;
      case SUBSTRING -> normalizedField + "_txt";
    };
  }

  /**
   * Determines if this mode requires value escaping for Solr queries.
   *
   * @return true if values should be escaped (PHRASE mode), false for SUBSTRING
   */
  public boolean requiresValueEscaping() {
    return this == PHRASE;
  }

  /**
   * Returns a human-readable description of this search mode.
   *
   * @return Description suitable for API documentation or UI tooltips
   */
  public String getDescription() {
    return switch (this) {
      case PHRASE -> "Exact phrase matching (case-insensitive)";
      case SUBSTRING -> "Flexible substring matching with tokenization";
    };
  }
}