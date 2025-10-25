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

  /**
   * URL-encodes Solr special characters in query values.
   * CRITICAL: Must URL-encode to prevent "Illegal character" errors in URIs
   * @param url The input string to encode
   * @return The URL-encoded value
   */
  public static String urlEncodeSolrSpecialCharacters(String url) {
    if (url == null) {
      return "";
    }

    return url
        .replace("{", "%7B")
        .replace("}", "%7D")
        .replace("|", "%7C")
        .replace("!", "%21")
        .replace(" ", "%20");
  }

}
