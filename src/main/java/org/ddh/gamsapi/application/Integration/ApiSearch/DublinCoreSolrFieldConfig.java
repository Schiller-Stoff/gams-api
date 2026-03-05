package org.ddh.gamsapi.application.Integration.ApiSearch;

/**
 * Configuration for Dublin Core field naming conventions in Solr.
 *
 * <p><b>Multi-Language Strategy (Strategy 4: Hybrid)</b></p>
 *
 * <p>Dublin Core fields are indexed with three concerns:</p>
 * <ol>
 *   <li><b>Search:</b> Combined field {@code dc.title} contains all language values (clean, no language prefix)
 *       for cross-language fulltext search and faceting.</li>
 *   <li><b>Fulltext:</b> Tokenized field {@code dc.title_txt} (via Solr copyField) enables substring matching.</li>
 *   <li><b>Display:</b> Language-specific fields like {@code dc.title.en} store clean values per language,
 *       enabling clients to display the appropriate language without parsing.</li>
 * </ol>
 *
 * <h3>Naming Convention</h3>
 * <p>Language-specific fields follow the natural Dublin Core dot-notation:</p>
 * <pre>
 *   dc.{dcFieldName}.{languageCode}
 *   e.g., dc.title.en, dc.title.de, dc.subject.la
 * </pre>
 *
 * <p>Values without a language tag are indexed under the "undefined" language code {@code und}
 * (ISO 639-2 standard for undetermined language), e.g., {@code dc.title.und}.</p>
 *
 * <h3>Solr Dynamic Field</h3>
 * <p>These fields are matched by the dynamic field pattern:</p>
 * <pre>{@code <dynamicField name="dc.*" type="strings" indexed="false" stored="true" multiValued="true"/>}</pre>
 * <p>Solr resolves fields by checking explicit definitions first, then dynamic fields.
 * Since all 15 Dublin Core fields (dc.title, dc.creator, etc.) are explicitly defined,
 * they always match their explicit definitions. Language-specific fields like dc.title.en
 * have no explicit definition, so they fall through to the {@code dc.*} dynamic field.</p>
 *
 * <p>Note: Solr only supports a single wildcard in dynamic field patterns (no {@code dc.*.*}),
 * so {@code dc.*} is the correct pattern. Explicit field priority makes this safe.</p>
 *
 * <p>{@code indexed="false"} because these fields are for display only — searching is done against
 * the combined {@code dc.*} fields (which are explicitly defined with {@code indexed="true"}).</p>
 *
 * @see ApiSearchService#addDublinCore
 */
public final class DublinCoreSolrFieldConfig {

  private DublinCoreSolrFieldConfig() {
    // Utility class — no instantiation
  }

  /**
   * Prefix for the combined (all-language) Dublin Core search fields.
   * Example: dc.title, dc.creator
   */
  public static final String DC_SEARCH_PREFIX = "dc.";

  /**
   * Prefix for language-specific Dublin Core display fields.
   * Uses the same "dc." prefix as the combined fields, with an additional language code suffix.
   * Example: dc.title.en, dc.creator.de, dc.subject.und
   *
   * <p>This does NOT collide with existing fields because:</p>
   * <ul>
   *   <li>Explicit field definitions (dc.title) take priority over dynamic fields in Solr</li>
   *   <li>copyField rules match explicit source names only (dc.title → dc.title_txt)</li>
   *   <li>The dynamic field {@code dc.*} only catches field names with no explicit definition</li>
   * </ul>
   */
  public static final String DC_LANG_PREFIX = "dc.";

  /**
   * ISO 639-2 code for "undetermined" language.
   * Used when a Dublin Core entry has no language specified.
   */
  public static final String UNDEFINED_LANGUAGE_CODE = "und";

  /**
   * Builds the language-specific Solr field name for a Dublin Core entry.
   *
   * @param dcFieldName Dublin Core field name without prefix (e.g., "title", "creator")
   * @param language    Language code (e.g., "en", "de"), or null/empty for undefined
   * @return Solr field name (e.g., "dc.title.en" or "dc.title.und")
   */
  public static String buildLanguageFieldName(String dcFieldName, String language) {
    String langCode = (language != null && !language.isBlank())
        ? language.trim().toLowerCase()
        : UNDEFINED_LANGUAGE_CODE;
    return DC_LANG_PREFIX + dcFieldName + "." + langCode;
  }

  /**
   * Builds the combined (all-language) Solr field name for a Dublin Core entry.
   *
   * @param dcFieldName Dublin Core field name without prefix (e.g., "title", "creator")
   * @return Solr field name (e.g., "dc.title")
   */
  public static String buildSearchFieldName(String dcFieldName) {
    return DC_SEARCH_PREFIX + dcFieldName;
  }

}