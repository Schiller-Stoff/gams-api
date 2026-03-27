package org.ddh.gamsapi.application.Integration.SemanticSearch.utils;

import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TurtleSparqlConverterTest extends UnitTest {

  @Test
  @DisplayName("Should convert standard Turtle @prefix declarations to SPARQL syntax")
  void separatePrefixesAndTriples_WithTurtlePrefix_ShouldConvert() {
    String input = "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
        "  @prefix   ex:  <http://example.org/>  .\n" + // Testing irregular spacing
        "ex:subject ex:predicate \"value\" .";

    TurtleParseResult result = TurtleSparqlConverter.separatePrefixesAndTriples(input);

    assertEquals(2, result.sparqlPrefixes().size(), "Should extract 2 prefixes");
    assertTrue(result.sparqlPrefixes().contains("PREFIX foaf: <http://xmlns.com/foaf/0.1/>"));
    assertTrue(result.sparqlPrefixes().contains("PREFIX ex: <http://example.org/>"));

    // Ensure triples are separated cleanly
    assertEquals("ex:subject ex:predicate \"value\" .\n", result.triples());
    assertFalse(result.hasNoTriples());
  }

  @Test
  @DisplayName("Should convert standard Turtle @base declarations to SPARQL syntax")
  void separatePrefixesAndTriples_WithTurtleBase_ShouldConvert() {
    String input = "@base <http://example.org/base/> .\n" +
        "<#subject> <#predicate> \"value\" .";

    TurtleParseResult result = TurtleSparqlConverter.separatePrefixesAndTriples(input);

    assertEquals(1, result.sparqlPrefixes().size());
    assertTrue(result.sparqlPrefixes().contains("BASE <http://example.org/base/>"));
    assertEquals("<#subject> <#predicate> \"value\" .\n", result.triples());
  }

  @Test
  @DisplayName("Should pass through existing SPARQL PREFIX and BASE declarations unchanged")
  void separatePrefixesAndTriples_WithExistingSparqlSyntax_ShouldPassThrough() {
    String input = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
        "BASE <http://example.org/>\n" +
        "ex:s ex:p ex:o .";

    TurtleParseResult result = TurtleSparqlConverter.separatePrefixesAndTriples(input);

    assertEquals(2, result.sparqlPrefixes().size());
    assertTrue(result.sparqlPrefixes().contains("PREFIX foaf: <http://xmlns.com/foaf/0.1/>"));
    assertTrue(result.sparqlPrefixes().contains("BASE <http://example.org/>"));
    assertEquals("ex:s ex:p ex:o .\n", result.triples());
  }

  @Test
  @DisplayName("Should return empty sets and hasNoTriples=true for empty or blank input")
  void separatePrefixesAndTriples_EmptyInput_ShouldHandleGracefully() {
    TurtleParseResult emptyResult = TurtleSparqlConverter.separatePrefixesAndTriples("");
    assertTrue(emptyResult.sparqlPrefixes().isEmpty());
    assertTrue(emptyResult.hasNoTriples());

    TurtleParseResult blankResult = TurtleSparqlConverter.separatePrefixesAndTriples("   \n  \n");
    assertTrue(blankResult.sparqlPrefixes().isEmpty());
    assertTrue(blankResult.hasNoTriples()); // Only contains whitespace/newlines
  }

  @Test
  @DisplayName("mergePrefixes: Should deduplicate and merge multiple prefix sets")
  void mergePrefixes_ShouldDeduplicateAndMerge() {
    Set<String> set1 = Set.of(
        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>",
        "PREFIX ex: <http://example.org/>"
    );
    Set<String> set2 = Set.of(
        "PREFIX ex: <http://example.org/>", // Duplicate
        "PREFIX dc: <http://purl.org/dc/elements/1.1/>"
    );

    Set<String> merged = TurtleSparqlConverter.mergePrefixes(set1, set2);

    assertEquals(3, merged.size());
    assertTrue(merged.contains("PREFIX foaf: <http://xmlns.com/foaf/0.1/>"));
    assertTrue(merged.contains("PREFIX ex: <http://example.org/>"));
    assertTrue(merged.contains("PREFIX dc: <http://purl.org/dc/elements/1.1/>"));
  }

  @Test
  @DisplayName("TurtleParseResult.hasNoTriples: Should accurately identify missing triple data")
  void turtleParseResult_hasNoTriples_LogicCheck() {
    // True cases
    assertTrue(new TurtleParseResult(Set.of(), null).hasNoTriples());
    assertTrue(new TurtleParseResult(Set.of(), "").hasNoTriples());
    assertTrue(new TurtleParseResult(Set.of(), "   \n  \t ").hasNoTriples());

    // False cases
    assertFalse(new TurtleParseResult(Set.of(), "ex:s ex:p ex:o .").hasNoTriples());
  }
}