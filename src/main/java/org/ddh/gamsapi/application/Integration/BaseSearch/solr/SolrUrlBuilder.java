package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

public class SolrUrlBuilder {

  /**
   * Escapes special characters in Solr query values.
   * CRITICAL: Must properly escape to prevent query syntax errors.
   * TODO TEST ME
   */
  public static String escapeSolrValue(String value) {
    if (value == null) {
      return "";
    }

    // Escape Solr special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
    return value
        .replace("\\", "\\\\")  // Backslash FIRST
        .replace("\"", "\\\"")  // Quote
        .replace("+", "\\+")
        .replace("-", "\\-")
        .replace("&&", "\\&&")
        .replace("||", "\\||")
        .replace("!", "\\!")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("^", "\\^")
        .replace("~", "\\~")
        .replace("*", "\\*")
        .replace("?", "\\?")
        .replace(":", "\\:");
  }

}
