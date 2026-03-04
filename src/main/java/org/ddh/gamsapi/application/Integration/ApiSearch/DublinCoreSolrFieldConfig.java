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
 *   <li><b>Display:</b> Language-specific fields like {@code dc_lang.title.en} store clean values per language,
 *       enabling clients to display the appropriate language without parsing.</li>
 * </ol>
 *
 * <h3>Naming Convention</h3>
 * <p>Language-specific fields use the prefix {@code dc_lang.} to avoid collisions with existing
 * {@code dc.*_txt} copyField rules in the Solr schema. The pattern is:</p>
 * <pre>
 *   dc_lang.{dcFieldName}.{languageCode}
 *   e.g., dc_lang.title.en, dc_lang.title.de, dc_lang.subject.la
 * </pre>
 *
 * <p>Values without a language tag are indexed under the "undefined" language code {@code und}
 * (ISO 639-2 standard for undetermined language).</p>
 *
 * <h3>Solr Dynamic Field</h3>
 * <p>These fields are matched by the dynamic field pattern:</p>
 * <pre>{@code <dynamicField name="dc_lang.*" type="strings" indexed="false" stored="true" multiValued="true"/>}</pre>
 * <p>Note: {@code indexed="false"} because these fields are for display only — searching is done against
 * the combined {@code dc.*} fields.</p>
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
   * Uses "dc_lang." to avoid collisions with the existing "dc.*_txt" copyField rules.
   * Example: dc_lang.title.en, dc_lang.creator.de
   */
  public static final String DC_LANG_PREFIX = "dc_lang.";

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
   * @return Solr field name (e.g., "dc_lang.title.en" or "dc_lang.title.und")
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