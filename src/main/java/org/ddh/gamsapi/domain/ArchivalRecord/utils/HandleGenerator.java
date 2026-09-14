package org.ddh.gamsapi.domain.ArchivalRecord.utils;

import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Generates handle PIDs for tests, e.g. {@code hdl:11471/518.10.1.4714}.
 * <p>
 * Two modes:
 * <ul>
 *   <li>{@link #generate()} - random, collision-free within a JVM run. Use when the handle
 *       value itself is irrelevant and you only need a distinct, syntactically valid PID.</li>
 *   <li>{@link #deriveFrom(String)} - deterministic, a pure function of the digital object id.
 *       Use when a test must assert a specific handle, or when two independent parts of a test
 *       have to agree on the same handle without passing it around.</li>
 * </ul>
 * <b>The deterministic mode is a hash into a bounded numeric space and is therefore
 * collision-prone at scale.</b> It is safe for the handful of objects in a test fixture; it is
 * not an allocation strategy for production. It is also one-way - the digital object id cannot
 * be recovered from the handle.
 */
public final class HandleGenerator {

  /** Handle URI scheme. */
  public static final String SCHEME = "hdl";

  /** Handle naming authority (prefix) of the University of Graz. */
  public static final String NAMING_AUTHORITY = "11471";

  /**
   * Matches the handles produced here and, more generally, any {@code hdl:<prefix>/<suffix>}
   * with a dot-separated numeric suffix. Useful for assertions.
   */
  public static final Pattern PATTERN =
      Pattern.compile("^hdl:(?<prefix>\\d+)/(?<suffix>\\d+(?:\\.\\d+)*)$");

  /**
   * Distinguishes concurrently running test JVMs. Process ids are unique among processes that
   * run at the same time, which is exactly the parallel-fork case.
   */
  private static final long FORK_DISCRIMINATOR = ProcessHandle.current().pid() % 1_000L;

  /** Starts at a random point so two sequential runs against the same database do not overlap. */
  private static final AtomicLong SEQUENCE =
      new AtomicLong(ThreadLocalRandom.current().nextInt(1_000, 9_000));

  private HandleGenerator() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  // ---------------------------------------------------------------------
  // Random generation
  // ---------------------------------------------------------------------

  /**
   * Generates a random handle PID using the default naming authority.
   *
   * @return a handle such as {@code hdl:11471/518.10.1.4714}
   */
  public static String generate() {
    return generate(NAMING_AUTHORITY);
  }

  /**
   * Generates a random handle PID for an explicit naming authority.
   *
   * @param namingAuthority the handle prefix, e.g. {@code "11471"}
   * @return a handle such as {@code hdl:11471/518.10.1.4714}
   * @throws IllegalArgumentException if the naming authority is null, blank or non-numeric
   */
  public static String generate(String namingAuthority) {
    requireNumericAuthority(namingAuthority);

    var random = ThreadLocalRandom.current();

    return format(
        namingAuthority,
        random.nextInt(100, 1_000),
        random.nextInt(10, 100),
        FORK_DISCRIMINATOR,
        SEQUENCE.incrementAndGet()
    );
  }

  // ---------------------------------------------------------------------
  // Deterministic derivation from a digital object
  // ---------------------------------------------------------------------

  /**
   * Derives a stable handle PID from a digital object's id.
   * The same object always yields the same handle, across runs and machines.
   *
   * @param digitalObject the object to derive from
   * @return a handle such as {@code hdl:11471/518.10.1.4714}
   * @throws IllegalArgumentException if the object or its id is null or empty
   */
  public static String deriveFrom(DigitalObject digitalObject) {
    if (digitalObject == null) {
      throw new IllegalArgumentException("Digital object must not be null");
    }
    return deriveFrom(digitalObject.getId());
  }

  /**
   * Derives a stable handle PID from a digital object id, using the default naming authority.
   *
   * @param digitalObjectId e.g. {@code "demo.manuscript-001"} or legacy {@code "o:demo.1"}
   * @return a handle such as {@code hdl:11471/518.10.1.4714}
   * @throws IllegalArgumentException if the id is null or empty
   */
  public static String deriveFrom(String digitalObjectId) {
    return deriveFrom(digitalObjectId, NAMING_AUTHORITY);
  }

  /**
   * Derives a stable handle PID from a digital object id for an explicit naming authority.
   * <p>
   * The id is normalised first: a legacy type prefix ({@code o:}, {@code p:}) is stripped and
   * the remainder is lower-cased, so {@code "o:demo.1"} and {@code "demo.1"} map to the same
   * handle. SHA-256 of that normalised id is then sliced into four numeric segments matching
   * the {@code 518.10.1.4714} shape.
   *
   * @param digitalObjectId the digital object id
   * @param namingAuthority the handle prefix, e.g. {@code "11471"}
   * @return a handle such as {@code hdl:11471/518.10.1.4714}
   * @throws IllegalArgumentException if the id is null or empty, or the authority is non-numeric
   */
  public static String deriveFrom(String digitalObjectId, String namingAuthority) {
    if (digitalObjectId == null || digitalObjectId.isBlank()) {
      throw new IllegalArgumentException("Digital object id must not be null or empty");
    }
    requireNumericAuthority(namingAuthority);

    byte[] digest = sha256(normalise(digitalObjectId));

    // Four disjoint 4-byte slices, so the segments do not correlate with each other.
    return format(
        namingAuthority,
        100 + unsignedInt(digest, 0) % 900,      // 3 digits, e.g. 518
        10 + unsignedInt(digest, 4) % 90,        // 2 digits, e.g. 10
        1 + unsignedInt(digest, 8) % 9,          // 1 digit,  e.g. 1
        1_000 + unsignedInt(digest, 12) % 9_000  // 4 digits, e.g. 4714
    );
  }

  // ---------------------------------------------------------------------
  // Inspection
  // ---------------------------------------------------------------------

  /**
   * Checks whether a string is a syntactically valid handle PID.
   *
   * @param handle the candidate, may be null
   * @return {@code true} if it matches {@link #PATTERN}
   */
  public static boolean isValid(String handle) {
    return handle != null && PATTERN.matcher(handle).matches();
  }

  /**
   * Extracts the suffix (local name) of a handle, e.g. {@code 518.10.1.4714}.
   *
   * @param handle a handle PID
   * @return the part after the slash
   * @throws IllegalArgumentException if the handle is not valid
   */
  public static String deriveSuffix(String handle) {
    var matcher = PATTERN.matcher(handle == null ? "" : handle);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Not a valid handle PID: " + handle);
    }
    return matcher.group("suffix");
  }

  // ---------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------

  private static String format(String namingAuthority, long a, long b, long c, long d) {
    return "%s:%s/%d.%d.%d.%d".formatted(SCHEME, namingAuthority, a, b, c, d);
  }

  private static void requireNumericAuthority(String namingAuthority) {
    if (namingAuthority == null || !namingAuthority.matches("\\d+")) {
      throw new IllegalArgumentException(
          "Naming authority must be a non-empty numeric string, got: " + namingAuthority);
    }
  }

  /**
   * Strips a legacy type prefix ({@code o:test.1} to {@code test.1}) and lower-cases the rest,
   * mirroring the normalisation in {@code DigitalObjectIdValidator}.
   */
  private static String normalise(String digitalObjectId) {
    String id = digitalObjectId.strip();
    int colon = id.indexOf(':');
    if (colon >= 0) {
      id = id.substring(colon + 1);
    }
    return id.toLowerCase();
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by the JDK spec; this cannot happen.
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /** Reads four bytes at {@code offset} as an unsigned 32-bit value. */
  private static long unsignedInt(byte[] bytes, int offset) {
    return ((long) (bytes[offset] & 0xFF) << 24)
        | ((long) (bytes[offset + 1] & 0xFF) << 16)
        | ((long) (bytes[offset + 2] & 0xFF) << 8)
        | (bytes[offset + 3] & 0xFF);
  }

}
