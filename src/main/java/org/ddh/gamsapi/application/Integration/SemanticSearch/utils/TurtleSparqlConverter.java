package org.ddh.gamsapi.application.Integration.SemanticSearch.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting Turtle content into a format suitable for SPARQL UPDATE INSERT DATA.
 * <p>
 * Turtle and SPARQL use different prefix declaration syntax:
 * <ul>
 *   <li>Turtle:  {@code @prefix foaf: <http://xmlns.com/foaf/0.1/> .}</li>
 *   <li>SPARQL:  {@code PREFIX foaf: <http://xmlns.com/foaf/0.1/>}</li>
 * </ul>
 * <p>
 * The INSERT DATA block requires PREFIX declarations to appear <em>before</em>
 * the INSERT DATA keyword. They cannot appear inside the {@code { }} block.
 * This class extracts prefix/base declarations from Turtle content, converts
 * them to SPARQL syntax, and returns the remaining triple statements separately.
 */
public final class TurtleSparqlConverter {

  private TurtleSparqlConverter() {
    // utility class
  }

  /**
   * Pattern matching Turtle @prefix declarations.
   * Captures the prefix name and IRI, e.g.:
   * {@code @prefix foaf: <http://xmlns.com/foaf/0.1/> .}
   * Group 1: prefix name with colon (e.g. "foaf:")
   * Group 2: the IRI including angle brackets (e.g. "<http://xmlns.com/foaf/0.1/>")
   */
  private static final Pattern TURTLE_PREFIX_PATTERN =
      Pattern.compile("^\\s*@prefix\\s+(\\S+:)\\s+(<[^>]+>)\\s*\\.\\s*$", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern matching Turtle @base declarations.
   * {@code @base <http://example.org/> .}
   */
  private static final Pattern TURTLE_BASE_PATTERN =
      Pattern.compile("^\\s*@base\\s+(<[^>]+>)\\s*\\.\\s*$", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern matching SPARQL-style PREFIX declarations (already valid, pass through).
   * {@code PREFIX foaf: <http://xmlns.com/foaf/0.1/>}
   */
  private static final Pattern SPARQL_PREFIX_PATTERN =
      Pattern.compile("^\\s*PREFIX\\s+\\S+:\\s+<[^>]+>\\s*$", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern matching SPARQL-style BASE declarations (already valid, pass through).
   * {@code BASE <http://example.org/>}
   */
  private static final Pattern SPARQL_BASE_PATTERN =
      Pattern.compile("^\\s*BASE\\s+<[^>]+>\\s*$", Pattern.CASE_INSENSITIVE);


  /**
   * Separates a Turtle string into SPARQL-compatible prefix declarations and triple data.
   * <p>
   * Turtle {@code @prefix} and {@code @base} declarations are converted to SPARQL syntax.
   * SPARQL-style PREFIX/BASE declarations are passed through unchanged.
   * Everything else is treated as triple data.
   *
   * @param turtleContent raw Turtle content from a datastream
   * @return a {@link TurtleParseResult} with separated prefixes (in SPARQL syntax) and triples
   */
  public static TurtleParseResult separatePrefixesAndTriples(String turtleContent) {

    Set<String> sparqlPrefixes = new HashSet<>();
    StringBuilder triples = new StringBuilder();

    for (String line : turtleContent.split("\\R")) { // \\R matches any line break

      // 1. Try Turtle @prefix → convert to SPARQL PREFIX
      Matcher prefixMatcher = TURTLE_PREFIX_PATTERN.matcher(line);
      if (prefixMatcher.matches()) {
        String sparqlPrefix = String.format("PREFIX %s %s",
            prefixMatcher.group(1), prefixMatcher.group(2));
        sparqlPrefixes.add(sparqlPrefix);
        continue;
      }

      // 2. Try Turtle @base → convert to SPARQL BASE
      Matcher baseMatcher = TURTLE_BASE_PATTERN.matcher(line);
      if (baseMatcher.matches()) {
        String sparqlBase = String.format("BASE %s", baseMatcher.group(1));
        sparqlPrefixes.add(sparqlBase);
        continue;
      }

      // 3. Already SPARQL PREFIX → pass through
      if (SPARQL_PREFIX_PATTERN.matcher(line).matches()) {
        sparqlPrefixes.add(line.trim());
        continue;
      }

      // 4. Already SPARQL BASE → pass through
      if (SPARQL_BASE_PATTERN.matcher(line).matches()) {
        sparqlPrefixes.add(line.trim());
        continue;
      }

      // 5. Everything else is triple data
      triples.append(line).append("\n");
    }

    return new TurtleParseResult(sparqlPrefixes, triples.toString());
  }

  /**
   * Merges multiple sets of SPARQL prefix declarations into one deduplicated set.
   * Useful when aggregating prefixes from multiple Turtle files into a single
   * INSERT DATA request.
   *
   * @param prefixSets multiple prefix sets to merge
   * @return a single deduplicated set of PREFIX declarations
   */
  @SafeVarargs
  public static Set<String> mergePrefixes(Set<String>... prefixSets) {
    Set<String> merged = new HashSet<>();
    for (Set<String> prefixSet : prefixSets) {
      merged.addAll(prefixSet);
    }
    return merged;
  }
}