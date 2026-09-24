package org.ddh.gamsapi.domain.ArchivalRecord.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleAlreadyExistsException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleNotRegisteredException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleServerException;
import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Basic-auth variant of the handle client (manual 14.6.3): every write request carries
 * {@code Authorization: Basic ...} with an HS_SECKEY identity. No sessions, no signatures.
 * <p>
 * Requires one extra property: {@code gams.services.handle.secret-key-path} (file with the secret,
 * e.g. a Docker secret). {@code private-key-path} is then unused.
 */
@Component
@Slf4j
public class BasicAuthHandleClient implements IHandleClient {

  private final HandleServerProperties properties;
  private final RestClient restClient;
  private final URI baseUri;

  public BasicAuthHandleClient(HandleServerProperties properties, ObjectProvider<SslBundles> sslBundles) {
    this.properties = properties;
    this.baseUri = URI.create(properties.getBaseUrl().replaceAll("/+$", ""));

    var httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(properties.getConnectTimeout());
    if (StringUtils.hasText(properties.getSslBundle())) {
      httpClient.sslContext(sslBundles.getObject().getBundle(properties.getSslBundle()).createSslContext());
    }
    var requestFactory = new JdkClientHttpRequestFactory(httpClient.build());
    requestFactory.setReadTimeout(properties.getReadTimeout());

    this.restClient = RestClient.builder()
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public String generate() {
    return HandleGenerator.generate(properties.getPrefix());
  }

  @Override
  public void register(String pid, URI target) {
    String handle = toHandle(pid);
    var body = new Values(List.of(adminValue(), urlValue(target)));
    int status = send(HttpMethod.PUT, handleUri(handle, "overwrite", "false"), body, true).status().value();

    if (status == 201 || status == 200) {
      log.info("Registered handle {} -> {}", handle, target);
    } else if (status == 409) {
      // idempotent retry: fine if it already points to the same target
      if (!resolveTarget(pid).map(target::equals).orElse(false)) {
        throw new HandleAlreadyExistsException("Handle " + handle + " already exists with a different target.");
      }
    } else {
      throw failed("register", handle, status);
    }
  }

  @Override
  public void retarget(String pid, URI target) {
    String handle = toHandle(pid);
    if (!exists(pid)) {
      throw new HandleNotRegisteredException("Cannot retarget handle " + handle + " - it does not exist.");
    }
    var uri = handleUri(handle, "index", String.valueOf(properties.getUrlValueIndex()), "overwrite", "true");
    int status = send(HttpMethod.PUT, uri, new Values(List.of(urlValue(target))), true).status().value();
    if (status != 200 && status != 201) {
      throw failed("retarget", handle, status);
    }
  }

  @Override
  public Optional<URI> resolveTarget(String pid) {
    String handle = toHandle(pid);
    var outcome = send(HttpMethod.GET, handleUri(handle, "index", String.valueOf(properties.getUrlValueIndex())), null, false);
    int status = outcome.status().value();
    if (status == 404) {
      return Optional.empty();
    }
    if (status != 200) {
      throw failed("resolve", handle, status);
    }
    if (outcome.body() == null || outcome.body().values() == null) {
      return Optional.empty();
    }
    return outcome.body().values().stream()
        .filter(v -> "URL".equalsIgnoreCase(v.type()))
        .map(Value::dataAsString)
        .filter(StringUtils::hasText)
        .findFirst()
        .map(URI::create);
  }

  @Override
  public boolean exists(String pid) {
    String handle = toHandle(pid);
    int status = send(HttpMethod.GET, handleUri(handle), null, false).status().value();
    if (status == 200) return true;
    if (status == 404) return false;
    throw failed("check existence of", handle, status);
  }

  @Override
  public void delete(String pid) {
    String handle = toHandle(pid);
    int status = send(HttpMethod.DELETE, handleUri(handle), null, true).status().value();
    if (status != 200 && status != 204 && status != 404) { // 404 = already gone
      throw failed("delete", handle, status);
    }
  }

  // ---------------------------------------------------------------------
  // Basic auth
  // ---------------------------------------------------------------------

  /**
   * Manual 14.6.3: username is {@code index:handle}, percent-encoded minimally
   * ('%' -> %25, then every ':' -> %3A, including the one between index and handle).
   * The secret is read per request so a rotated Docker secret takes effect without restart.
   */
  private String basicAuthHeader() {
    String username = properties.getAuthIndex() + "%3A"
        + properties.getAuthHandle().replace("%", "%25").replace(":", "%3A");
    String secret;
    try {
      secret = Files.readString(Path.of(properties.getSecretKeyPath()), StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      throw new HandleServerException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read handle secret key: " + e.getMessage(), e);
    }
    return "Basic " + Base64.getEncoder()
        .encodeToString((username + ":" + secret).getBytes(StandardCharsets.UTF_8));
  }

  // ---------------------------------------------------------------------
  // Transport + helpers
  // ---------------------------------------------------------------------

  private Outcome send(HttpMethod method, URI uri, @Nullable Object body, boolean authenticated) {
    try {
      RestClient.RequestBodySpec spec = restClient.method(method).uri(uri);
      if (authenticated) {
        spec = spec.header(HttpHeaders.AUTHORIZATION, basicAuthHeader());
      }
      if (body != null) {
        spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
      }
      Outcome outcome = spec.exchange((req, res) -> new Outcome(res.getStatusCode(), readLeniently(res)));
      int status = outcome.status().value();
      if (status == 401 || status == 403) {
        throw new HandleServerException(HttpStatus.INTERNAL_SERVER_ERROR, "Handle server rejected credentials for " + properties.getAuthIndex()
            + ":" + properties.getAuthHandle() + " (HTTP " + status + ") Response body: " + outcome.body());
      }
      return outcome;
    } catch (ResourceAccessException e) {
      throw new HandleServerException(HttpStatus.INTERNAL_SERVER_ERROR, "Handle server not reachable at " + baseUri + ": " + e.getMessage(), e);
    }
  }

  private static @Nullable Response readLeniently(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse res) {
    try {
      return res.bodyTo(Response.class);
    } catch (RestClientException | HttpMessageConversionException e) {
      return null; // empty body or non-JSON error page
    }
  }

  private String toHandle(String pid) {
    String handle = pid != null && pid.startsWith("hdl:") ? pid.substring(4) : pid;
    if (handle == null || !handle.startsWith(properties.getPrefix() + "/")) {
      throw new IllegalArgumentException("PID " + pid + " is not under prefix " + properties.getPrefix());
    }
    return handle;
  }

  private URI handleUri(String handle, String... query) {
    var builder = UriComponentsBuilder.fromUri(baseUri).path("/api/handles/").path(handle);
    for (int i = 0; i + 1 < query.length; i += 2) {
      builder.queryParam(query[i], query[i + 1]);
    }
    return builder.build().encode().toUri();
  }

  private Value adminValue() {
    return new Value(properties.getAdminValueIndex(), "HS_ADMIN", Map.of("format", "admin", "value", Map.of(
        "handle", properties.getAdminHandle(),
        "index", properties.getAdminIndex(),
        "permissions", properties.getAdminPermissions())), properties.getTtlSeconds());
  }

  private Value urlValue(URI target) {
    return new Value(properties.getUrlValueIndex(), "URL",
        Map.of("format", "string", "value", target.toString()), properties.getTtlSeconds());
  }

  private static HandleServerException failed(String action, String handle, int status) {
    return new HandleServerException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to " + action + " handle " + handle + " (HTTP " + status + ")");
  }

  // ---------------------------------------------------------------------
  // Wire DTOs (manual 14.8)
  // ---------------------------------------------------------------------

  record Outcome(HttpStatusCode status, @Nullable Response body) {}

  record Values(List<Value> values) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Value(Integer index, String type, Object data, Integer ttl) {
    @Nullable String dataAsString() {
      if (data instanceof String s) return s;
      if (data instanceof Map<?, ?> m && m.get("value") instanceof String s) return s;
      return null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Response(Integer responseCode, String message, List<Value> values) {}
}