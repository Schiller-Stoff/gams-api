package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import java.util.regex.Pattern;

/** Value object: a handle in canonical form "<prefix>/<suffix>". The suffix may contain '/'. */
public record Handle(String prefix, String suffix) {

  private static final Pattern PREFIX_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
  private static final Pattern SUFFIX_PATTERN = Pattern.compile("[\\x21-\\x7E]+"); // printable ASCII, no whitespace

  public Handle {
    if (prefix == null || !PREFIX_PATTERN.matcher(prefix).matches())
      throw new IllegalArgumentException("Invalid handle prefix: " + prefix);
    if (suffix == null || !SUFFIX_PATTERN.matcher(suffix).matches())
      throw new IllegalArgumentException("Invalid handle suffix: " + suffix);
  }

  /**
   * Parses given handle string to Handle object. Works with hdl:1123/foobar and 1123/foobar. Slash must be contained.
   * @param value handle as string
   * @return parsed handle
   */
  public static Handle parse(String value) {
    if(value.startsWith("hdl:")){
      value = value.substring("hdl:".length());
    }
    int slash = value == null ? -1 : value.indexOf('/');
    if (slash <= 0) throw new IllegalArgumentException("Not a handle: " + value);
    return new Handle(value.substring(0, slash), value.substring(slash + 1));
  }

  public static boolean isValid(String value) {
    try { parse(value); return true; } catch (IllegalArgumentException e) { return false; }
  }

  /**
   * Returns handle with prefixed hdl:
   * @return
   */
  public String toHdlUri() { return "hdl:" + this; }

  @Override public String toString() { return prefix + "/" + suffix; }
}
