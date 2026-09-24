package org.ddh.gamsapi.infrastructure.System.configproperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration of the connection to the (self-hosted) Handle.Net server.
 * Bound from {@code gams.services.handle.*}.
 * <p>
 * Authentication: HTTP Basic auth with an HS_SECKEY identity (manual 14.6.3), sent on every
 * state-changing request. The identity is {@code authIndex:authHandle}, e.g. {@code 300:11471/GAMS-API}.
 * <p>
 * Note: {@code @Validated} is required - without it the constraint annotations are silently ignored.
 */
@Configuration
@ConfigurationProperties(prefix = "gams.services.handle")
@Validated
@Getter
@Setter
public class HandleServerProperties {

  // ---------------------------------------------------------------------
  // Connection
  // ---------------------------------------------------------------------

  /**
   * Base URL of the handle server HTTP interface, e.g. {@code https://handle:8000}.
   * Must be https: the handle server ignores credentials sent over plain http (manual 14.6).
   */
  @NotBlank
  @Pattern(regexp = "^https://\\S+$", message = "must be an https URL - the handle server ignores authentication over http")
  private String baseUrl;

  /**
   * Optional name of a Spring Boot SSL bundle ({@code spring.ssl.bundle.pem.<name>}) used as truststore,
   * e.g. to trust the handle server's self-signed certificate. Leave empty if the certificate is publicly trusted.
   */
  private String sslBundle;

  @NotNull
  private Duration connectTimeout = Duration.ofSeconds(5);

  @NotNull
  private Duration readTimeout = Duration.ofSeconds(15);

  // ---------------------------------------------------------------------
  // Authentication (Basic auth with HS_SECKEY)
  // ---------------------------------------------------------------------

  /**
   * Handle holding gams-api's HS_SECKEY value, e.g. {@code 11471/GAMS-API}.
   */
  @NotBlank
  private String authHandle;

  /**
   * Index of the HS_SECKEY value on {@link #authHandle} (300 by convention).
   */
  @Positive
  private int authIndex = 300;

  /**
   * Path to a file containing the secret (the HS_SECKEY value), e.g. a Docker secret under {@code /run/secrets/}.
   * Surrounding whitespace / a trailing newline is stripped. The file is read on every authenticated request,
   * so a rotated secret takes effect without a restart. Never put the secret itself into application.yaml.
   */
  @NotBlank
  private String secretKeyPath;

  // ---------------------------------------------------------------------
  // Handle creation
  // ---------------------------------------------------------------------

  /**
   * Handle prefix gams-api is allowed to mint and modify handles under, e.g. {@code 11471}.
   * Requests for PIDs outside this prefix are rejected client-side.
   */
  @NotBlank
  @Pattern(regexp = "^\\d+(\\.\\d+)*$")
  private String prefix = "11471";

  /**
   * Handle referenced by the HS_ADMIN value written into every new handle - typically an admin group
   * (HS_VLIST) containing gams-api's identity and human admins, e.g. {@code 11471/ADMIN}.
   */
  @NotBlank
  private String adminHandle;

  /**
   * Index on {@link #adminHandle} referenced by HS_ADMIN (200 = HS_VLIST admin group by convention).
   */
  @Positive
  private int adminIndex = 200;

  /**
   * 12-char HS_ADMIN permission bitmask (manual 4.9): add handle, delete handle, add NA, delete NA,
   * modify values, remove values, add values, read values, modify admin, remove admin, add admin, list handles.
   */
  @NotBlank
  @Pattern(regexp = "^[01]{12}$")
  private String adminPermissions = "110011111111";

  /** Index of the HS_ADMIN value on new handles (100 by convention). */
  @Positive
  private int adminValueIndex = 100;

  /** Index of the URL value on new handles. */
  @Positive
  private int urlValueIndex = 1;

  /** TTL (seconds) of written handle values. */
  @PositiveOrZero
  private int ttlSeconds = 86400;

}