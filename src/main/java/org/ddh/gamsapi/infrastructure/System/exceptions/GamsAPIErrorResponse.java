package org.ddh.gamsapi.infrastructure.System.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response for the GAMS API.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Consistent JSON structure for all error responses</li>
 *   <li>No internal details leaked (class names, SQL, stack traces)</li>
 *   <li>Field-level errors only included when present (via JsonInclude)</li>
 *   <li>ISO 8601 timestamp with timezone for unambiguous parsing</li>
 * </ul>
 *
 * <p>Example response:
 * <pre>{@code
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation error",
 *   "timestamp": "2025-03-04T10:15:30Z",
 *   "fieldErrors": [
 *     {"field": "title", "message": "must not be empty"},
 *     {"field": "creator", "message": "must not be null"}
 *   ]
 * }
 * }</pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GamsAPIErrorResponse {

  private final int status;
  private final String error;
  private final String message;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private final Instant timestamp;

  /**
   * Field-level validation errors. Only present when there are actual field errors.
   * Null (and thus omitted from JSON) when no field-level errors exist.
   */
  private final List<FieldErrorDetail> fieldErrors;

  public GamsAPIErrorResponse(HttpStatus status, String message) {
    this.status = status.value();
    this.error = status.getReasonPhrase();
    this.message = message;
    this.timestamp = Instant.now();
    this.fieldErrors = null;
  }

  public GamsAPIErrorResponse(HttpStatus status, String message, List<FieldErrorDetail> fieldErrors) {
    this.status = status.value();
    this.error = status.getReasonPhrase();
    this.message = message;
    this.timestamp = Instant.now();
    this.fieldErrors = (fieldErrors != null && !fieldErrors.isEmpty()) ? fieldErrors : null;
  }

  /**
   * Represents a single field-level validation error.
   * Deliberately excludes rejected value to prevent information leakage.
   */
  @Getter
  public static class FieldErrorDetail {
    private final String field;
    private final String message;

    public FieldErrorDetail(String field, String message) {
      this.field = field;
      this.message = message;
    }
  }
}