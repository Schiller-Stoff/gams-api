package org.ddh.gamsapi.application.Integration.SemanticSearch.utils;

import java.util.Set;

/**
 * Holds the result of parsing a Turtle file into its prefix declarations
 * and triple statements.
 * <p>
 * The prefixes are already converted from Turtle syntax ({@code @prefix foo: <...> .})
 * to SPARQL syntax ({@code PREFIX foo: <...>}) so they can be placed directly
 * before an INSERT DATA block.
 *
 * @param sparqlPrefixes prefix declarations in SPARQL syntax (deduplicated across files via Set)
 * @param triples        the triple statements without any prefix/base declarations
 */
public record TurtleParseResult(
    Set<String> sparqlPrefixes,
    String triples
) {

  /**
   * @return true if the triples section contains no meaningful content
   */
  public boolean hasNoTriples() {
    return triples == null || triples.isBlank();
  }
}