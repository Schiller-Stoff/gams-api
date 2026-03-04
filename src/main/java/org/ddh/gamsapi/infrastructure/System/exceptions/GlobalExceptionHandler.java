package org.ddh.gamsapi.infrastructure.System.exceptions;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Global exception handler for the GAMS API.
 *
 * <p>Design principles:
 * <ul>
 *   <li><b>No cause-chain walking:</b> Each exception type has a dedicated handler.
 *       Spring resolves the most specific @ExceptionHandler match, so wrapped exceptions
 *       like TransactionSystemException are unwrapped once (O(1)) instead of walked (O(n)).</li>
 *   <li><b>Security:</b> Internal details (SQL, class names, field values) are never exposed
 *       in responses. They are logged server-side at appropriate levels.</li>
 *   <li><b>Consistent response shape:</b> All errors return {@link GamsAPIErrorResponse}.</li>
 *   <li><b>Log levels follow HTTP semantics:</b> 4xx → DEBUG/WARN, 5xx → ERROR with stack trace.</li>
 * </ul>
 *
 * <p>Handler priority (Spring resolves most specific first):
 * <ol>
 *   <li>GamsApiException (and all subclasses) — application-level errors with known status</li>
 *   <li>ConstraintViolationException — Bean Validation at controller or JPA level</li>
 *   <li>MethodArgumentNotValidException — @Valid on @RequestBody</li>
 *   <li>TransactionSystemException — unwraps to find ConstraintViolationException</li>
 *   <li>DataIntegrityViolationException — DB constraint violations (unique, FK, etc.)</li>
 *   <li>HttpMessageNotReadableException — malformed JSON</li>
 *   <li>MethodArgumentTypeMismatchException — wrong type in path/query params</li>
 *   <li>MissingServletRequestParameterException — missing required query params</li>
 *   <li>HttpMediaTypeNotSupportedException — unsupported Content-Type (e.g., missing body on @RequestBody)</li>
 *   <li>HttpMediaTypeNotAcceptableException — cannot produce requested Accept type</li>
 *   <li>HttpRequestMethodNotSupportedException — wrong HTTP method for endpoint</li>
 *   <li>NoResourceFoundException — no handler found for request (static resources)</li>
 *   <li>Exception (catch-all) — unexpected errors, guaranteed consistent response</li>
 * </ol>
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // ─── Application Exceptions (GamsApiException hierarchy) ───────────────

  /**
   * Handles all custom application exceptions.
   * GamsApiException carries its own HTTP status, so we trust it directly.
   */
  @ExceptionHandler(GamsApiException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleGamsApiException(
      GamsApiException ex, WebRequest request) {

    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    HttpStatus effectiveStatus = (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR;

    if (effectiveStatus.is5xxServerError()) {
      log.error("Server error: {} - {}", effectiveStatus, ex.getReason(), ex);
    } else {
      log.debug("Client error: {} - {}", effectiveStatus, ex.getReason());
    }

    GamsAPIErrorResponse errorResponse = new GamsAPIErrorResponse(effectiveStatus, ex.getReason());

    HttpHeaders headers = new HttpHeaders();
    if (effectiveStatus == HttpStatus.NOT_FOUND) {
      headers.setCacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES));
    }

    return ResponseEntity.status(effectiveStatus).headers(headers).body(errorResponse);
  }

  // ─── Validation Exceptions ─────────────────────────────────────────────

  /**
   * Handles Bean Validation constraint violations thrown directly by @Validated
   * controllers or from manual validator invocations.
   *
   * <p>Security: Only exposes property path + validation message.
   * Never exposes the invalid value or the validated class name.</p>
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex) {

    List<GamsAPIErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
        .map(violation -> new GamsAPIErrorResponse.FieldErrorDetail(
            extractLeafPropertyName(violation.getPropertyPath().toString()),
            violation.getMessage()
        ))
        .toList();

    log.debug("Constraint violation: {} violations", fieldErrors.size());

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        fieldErrors
    );
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles @Valid failures on @RequestBody parameters.
   * Spring's MethodArgumentNotValidException provides BindingResult with field errors.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {

    List<GamsAPIErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
        .getAllErrors().stream()
        .map(error -> {
          String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
          return new GamsAPIErrorResponse.FieldErrorDetail(fieldName, error.getDefaultMessage());
        })
        .toList();

    log.debug("Method argument validation failed: {} errors", fieldErrors.size());

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Validation failed",
        fieldErrors
    );
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles TransactionSystemException — typically wraps a ConstraintViolationException
   * that occurred during JPA flush (Hibernate validation at persist/merge time).
   *
   * <p>This replaces the old drillRootErrorCause approach. Instead of walking the full
   * chain in a while-loop, we do a single targeted unwrap of the most specific cause.</p>
   */
  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleTransactionSystemException(
      TransactionSystemException ex) {

    Throwable rootCause = ex.getMostSpecificCause();

    // Delegate to the ConstraintViolationException handler if that's the root cause
    if (rootCause instanceof ConstraintViolationException cve) {
      return handleConstraintViolation(cve);
    }

    // For any other root cause, treat as unexpected server error
    log.error("Transaction system error with unexpected root cause: {}",
        rootCause.getClass().getSimpleName(), ex);

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "A transaction error occurred. Please try again or contact support."
    );
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // ─── Data Layer Exceptions ─────────────────────────────────────────────

  /**
   * Handles database constraint violations (unique constraints, foreign key violations, etc.).
   *
   * <p>Classification uses PostgreSQL SQLState codes (ISO/IEC 9075) which are
   * stable across PostgreSQL major versions, unlike error message text which is
   * locale-dependent and can change between releases.</p>
   *
   * <p>This handler is intentionally generic — it only classifies by constraint *type*
   * (unique, FK, not-null, etc.), not by specific entity. Entity-specific error messages
   * belong in the service layer where domain context is available. If a
   * DataIntegrityViolationException reaches this handler, it means the service layer
   * did not catch it — the generic message serves as a safe fallback.</p>
   *
   * <p>Security: Raw SQL, constraint names, and table names are logged server-side
   * but never exposed to the client.</p>
   *
   * @see <a href="https://www.postgresql.org/docs/current/errcodes-appendix.html">PostgreSQL Error Codes</a>
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {

    // Log the full detail server-side for debugging
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

    String sqlState = extractSqlState(ex);
    String userMessage = classifyBySqlState(sqlState);

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.CONFLICT, userMessage);
    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  // ─── Request Parsing Exceptions ────────────────────────────────────────

  /**
   * Handles malformed JSON or type mismatches in request body.
   *
   * <p>Logged at DEBUG since this is a client error, not a server problem.
   * Under load from a misbehaving client, this avoids flooding ERROR logs.</p>
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {

    log.debug("Malformed request body: {}", ex.getMessage());

    String errorMessage = "Invalid request body";
    Throwable cause = ex.getCause();

    if (cause instanceof JsonParseException) {
      errorMessage = "Malformed JSON in request body";
    } else if (cause instanceof UnrecognizedPropertyException upe) {
      errorMessage = "Unknown property: '" + upe.getPropertyName() + "'";
    } else if (cause instanceof InvalidFormatException ife) {
      errorMessage = String.format("Invalid value for property '%s'",
          extractJsonPath(ife));
    } else if ((cause instanceof MismatchedInputException mie) && (!mie.getPath().isEmpty())) {
      errorMessage = String.format("Invalid type for property '%s'",
          extractJsonPath(mie));
    }

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles type mismatches in path variables or query parameters.
   * E.g., sending "abc" for an integer parameter.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex) {

    log.debug("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

    String requiredType = (ex.getRequiredType() != null)
        ? ex.getRequiredType().getSimpleName()
        : "unknown";

    String message = String.format("Parameter '%s' must be of type %s",
        ex.getName(), requiredType);

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.BAD_REQUEST, message);
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles missing required query parameters.
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException ex) {

    log.debug("Missing required parameter: {}", ex.getParameterName());

    String message = String.format("Required parameter '%s' of type %s is missing",
        ex.getParameterName(), ex.getParameterType());

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.BAD_REQUEST, message);
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles unsupported Content-Type header or missing request body.
   *
   * <p>This is commonly triggered when:
   * <ul>
   *   <li>A PATCH/POST/PUT is sent without a Content-Type header and no body</li>
   *   <li>A Content-Type is sent that no HttpMessageConverter can handle</li>
   * </ul>
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {

    log.debug("Unsupported media type: {}", ex.getContentType());

    String message = (ex.getContentType() != null)
        ? String.format("Content type '%s' is not supported. Supported types: %s",
        ex.getContentType(), ex.getSupportedMediaTypes())
        : "A request body with a valid Content-Type header is required";

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  /**
   * Handles cases where the server cannot produce a response matching the client's Accept header.
   */
  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex) {

    log.debug("Not acceptable media type requested: {}", ex.getMessage());

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.NOT_ACCEPTABLE,
        "The requested media type is not supported for this resource"
    );
    return new ResponseEntity<>(response, HttpStatus.NOT_ACCEPTABLE);
  }

  /**
   * Handles requests with unsupported HTTP methods (e.g., DELETE on a read-only endpoint).
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {

    log.debug("Method not allowed: {} (supported: {})", ex.getMethod(), ex.getSupportedMethods());

    String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
    return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Handles requests for non-existent static resources.
   * Spring Boot 3.x throws this instead of returning a default 404.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
    log.debug("No resource found: {}", ex.getMessage());

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.NOT_FOUND, "The requested resource was not found");

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES));

    return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(response);
  }

  // ─── Catch-All ─────────────────────────────────────────────────────────

  /**
   * Catch-all handler for any unhandled exception.
   *
   * <p>This is critical for:
   * <ul>
   *   <li>Guaranteeing a consistent GamsAPIErrorResponse shape for ALL errors</li>
   *   <li>Preventing Spring Boot's BasicErrorController from returning a different JSON structure</li>
   *   <li>Ensuring no internal details leak through default error pages</li>
   * </ul>
   *
   * <p>Always logged at ERROR with full stack trace since these are genuinely unexpected.</p>
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<GamsAPIErrorResponse> handleAllUnexpectedExceptions(
      Exception ex, WebRequest request) {

    log.error("Unexpected error processing request: {}", request.getDescription(false), ex);

    GamsAPIErrorResponse response = new GamsAPIErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again or contact support."
    );
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // ─── Private Helpers ───────────────────────────────────────────────────

  /**
   * Extracts the leaf property name from a Bean Validation property path.
   * E.g., "createDigitalObject.arg0.title" → "title"
   */
  private String extractLeafPropertyName(String propertyPath) {
    if (propertyPath == null || propertyPath.isEmpty()) {
      return "unknown";
    }
    int lastDot = propertyPath.lastIndexOf('.');
    return (lastDot >= 0) ? propertyPath.substring(lastDot + 1) : propertyPath;
  }

  /**
   * Extracts a dot-separated JSON path from Jackson's path reference list.
   * Filters null field names (array indices) to produce clean property paths.
   */
  private String extractJsonPath(JsonMappingException jme) {
    return jme.getPath().stream()
        .map(JsonMappingException.Reference::getFieldName)
        .filter(Objects::nonNull)
        .collect(Collectors.joining("."));
  }

  /**
   * Extracts the SQL state code from the exception cause chain.
   *
   * <p>The chain is typically:
   * {@code DataIntegrityViolationException → hibernate ConstraintViolationException → PSQLException}
   * where PSQLException implements {@link SQLException#getSQLState()}.</p>
   *
   * @return the 5-character SQLState code, or {@code null} if not found
   */
  private String extractSqlState(DataIntegrityViolationException ex) {
    Throwable cause = ex.getCause();
    while (cause != null) {
      if (cause instanceof SQLException sqlEx) {
        return sqlEx.getSQLState();
      }
      cause = cause.getCause();
    }
    return null;
  }

  /**
   * Classifies a DataIntegrityViolationException into a safe, user-facing message
   * using PostgreSQL SQLState codes (ISO/IEC 9075, Appendix A).
   *
   * <p>This classification is intentionally generic. Entity-specific messages
   * (e.g., "Cannot delete this project because it still has digital objects")
   * should be provided by the service layer, which has domain context.
   * This handler serves as a safe fallback for anything that slips through.</p>
   *
   * <p>Relevant PostgreSQL SQLState codes (class 23 — Integrity Constraint Violation):
   * <ul>
   *   <li>{@code 23505} — unique_violation</li>
   *   <li>{@code 23503} — foreign_key_violation</li>
   *   <li>{@code 23502} — not_null_violation</li>
   *   <li>{@code 23514} — check_violation</li>
   *   <li>{@code 23001} — restrict_violation</li>
   *   <li>{@code 23000} — integrity_constraint_violation (generic)</li>
   *   <li>{@code 22001} — string_data_right_truncation (value too long)</li>
   * </ul>
   *
   * @param sqlState the 5-character SQLState code (may be null)
   * @return a safe, user-facing error message
   * @see <a href="https://www.postgresql.org/docs/current/errcodes-appendix.html">PostgreSQL Error Codes</a>
   */
  private String classifyBySqlState(String sqlState) {
    if (sqlState == null) {
      return "A data conflict occurred";
    }

    return switch (sqlState) {
      case "23505" -> "A resource with the same identifier already exists";
      case "23503" -> "Cannot modify or delete this resource because other resources depend on it";
      case "23502" -> "A required field is missing";
      case "23514" -> "A data validation constraint was violated";
      case "23001" -> "Cannot modify or delete this resource due to existing references";
      case "22001" -> "A field value exceeds the maximum allowed length";
      default -> {
        if (sqlState.startsWith("23")) {
          yield "A data constraint was violated";
        }
        yield "A data conflict occurred";
      }
    };
  }
}