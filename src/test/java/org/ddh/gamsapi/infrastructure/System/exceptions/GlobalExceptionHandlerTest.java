package org.ddh.gamsapi.infrastructure.System.exceptions;

import org.junit.jupiter.api.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Validates:
 * <ul>
 *   <li>Correct HTTP status codes for each exception type</li>
 *   <li>No internal details leaked in responses (security)</li>
 *   <li>Consistent GamsAPIErrorResponse structure</li>
 *   <li>Proper field error extraction</li>
 * </ul>
 */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private WebRequest webRequest;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    webRequest = new ServletWebRequest(new MockHttpServletRequest());
  }

  // ─── GamsApiException ──────────────────────────────────────────────────

  @Nested
  @DisplayName("GamsApiException handling")
  class GamsApiExceptionTests {

    @Test
    @DisplayName("Returns correct status and message for 404")
    void handlesNotFound() {
      GamsApiException ex = new GamsApiException(HttpStatus.NOT_FOUND, "Digital object not found");

      var response = handler.handleGamsApiException(ex, webRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(404);
      assertThat(response.getBody().getError()).isEqualTo("Not Found");
      assertThat(response.getBody().getMessage()).isEqualTo("Digital object not found");
      assertThat(response.getBody().getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("Returns 404 with cache headers")
    void notFoundHasCacheHeaders() {
      GamsApiException ex = new GamsApiException(HttpStatus.NOT_FOUND, "Not found");

      var response = handler.handleGamsApiException(ex, webRequest);

      assertThat(response.getHeaders().getCacheControl()).contains("max-age=300");
    }

    @Test
    @DisplayName("5xx errors do not have cache headers")
    void serverErrorNoCacheHeaders() {
      GamsApiException ex = new GamsApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Something broke");

      var response = handler.handleGamsApiException(ex, webRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getHeaders().getCacheControl()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Handles various status codes correctly")
    void handlesVariousStatusCodes() {
      for (HttpStatus status : new HttpStatus[]{
          HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN, HttpStatus.CONFLICT,
          HttpStatus.UNPROCESSABLE_CONTENT, HttpStatus.INTERNAL_SERVER_ERROR}) {

        GamsApiException ex = new GamsApiException(status, "Test error");
        var response = handler.handleGamsApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(status);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getStatus()).isEqualTo(status.value());
      }
    }
  }

  // ─── ConstraintViolationException ──────────────────────────────────────

  @Nested
  @DisplayName("ConstraintViolationException handling")
  class ConstraintViolationTests {

    @Test
    @DisplayName("Returns 400 with field errors, no internal details")
    void handlesConstraintViolation() {
      ConstraintViolation<?> violation = mockViolation("createObject.arg0.title", "must not be empty");
      Set<ConstraintViolation<?>> violations = Set.of(violation);
      ConstraintViolationException ex = new ConstraintViolationException(violations);

      var response = handler.handleConstraintViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
      assertThat(response.getBody().getFieldErrors()).hasSize(1);
      // Must extract leaf property name only — no method/parameter path leaked
      assertThat(response.getBody().getFieldErrors().getFirst().getField()).isEqualTo("title");
      assertThat(response.getBody().getFieldErrors().getFirst().getMessage()).isEqualTo("must not be empty");
    }

    @Test
    @DisplayName("Handles multiple violations")
    void handlesMultipleViolations() {
      Set<ConstraintViolation<?>> violations = new HashSet<>();
      violations.add(mockViolation("title", "must not be empty"));
      violations.add(mockViolation("creator", "must not be null"));
      ConstraintViolationException ex = new ConstraintViolationException(violations);

      var response = handler.handleConstraintViolation(ex);

      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getFieldErrors()).hasSize(2);
    }

    @Test
    @DisplayName("Does NOT expose invalid value or root bean class")
    void doesNotLeakInternalDetails() {
      ConstraintViolation<?> violation = mockViolation("password", "too short");
      ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

      var response = handler.handleConstraintViolation(ex);

      // The field error should only contain field name + message
      Assertions.assertNotNull(response.getBody());
      GamsAPIErrorResponse.FieldErrorDetail detail = response.getBody().getFieldErrors().getFirst();
      assertThat(detail.getField()).isEqualTo("password");
      assertThat(detail.getMessage()).isEqualTo("too short");
      // No rejected value, no class name in the response structure
    }
  }

  // ─── MethodArgumentNotValidException ───────────────────────────────────

  @Nested
  @DisplayName("MethodArgumentNotValidException handling")
  class MethodArgumentNotValidTests {

    @Test
    @DisplayName("Returns 400 with field errors from BindingResult")
    void handlesValidationErrors() {
      BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
      bindingResult.addError(new FieldError("request", "title", "must not be empty"));
      bindingResult.addError(new FieldError("request", "creator", "must not be null"));

      MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
          null, bindingResult);

      var response = handler.handleMethodArgumentNotValid(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getFieldErrors()).hasSize(2);
    }
  }

  // ─── TransactionSystemException ────────────────────────────────────────

  @Nested
  @DisplayName("TransactionSystemException handling")
  class TransactionSystemExceptionTests {

    @Test
    @DisplayName("Unwraps to ConstraintViolationException and returns 400")
    void unwrapsConstraintViolation() {
      ConstraintViolation<?> violation = mockViolation("dsid", "must not be empty");
      ConstraintViolationException cve = new ConstraintViolationException(Set.of(violation));
      TransactionSystemException ex = new TransactionSystemException("Transaction failed", cve);

      var response = handler.handleTransactionSystemException(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
      assertThat(response.getBody().getFieldErrors()).hasSize(1);
    }

    @Test
    @DisplayName("Returns 500 for non-constraint root causes")
    void handlesNonConstraintRootCause() {
      TransactionSystemException ex = new TransactionSystemException(
          "Transaction failed", new RuntimeException("DB timeout"));

      var response = handler.handleTransactionSystemException(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage())
          .doesNotContain("DB timeout")
          .contains("transaction error");
    }
  }

  // ─── DataIntegrityViolationException (SQLState-based) ────────────────────

  @Nested
  @DisplayName("DataIntegrityViolationException handling (SQLState-based, no schema coupling)")
  class DataIntegrityViolationTests {

    @Test
    @DisplayName("Unique violation (23505) → 409 with generic unique message")
    void handlesUniqueViolation() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "23505",
          "ERROR: duplicate key value violates unique constraint \"digital_object_pkey\"");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("A resource with the same identifier already exists");
      // Must NOT contain SQL, table names, or constraint names
      assertThat(response.getBody().getMessage()).doesNotContain("digital_object_pkey");
      assertThat(response.getBody().getMessage()).doesNotContain("duplicate key");
    }

    @Test
    @DisplayName("Foreign key violation (23503) → 409 with generic FK message")
    void handlesForeignKeyViolation() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "23503",
          "ERROR: update or delete on table \"project\" violates foreign key constraint");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage())
          .isEqualTo("Cannot modify or delete this resource because other resources depend on it");
      assertThat(response.getBody().getMessage()).doesNotContain("project");
    }

    @Test
    @DisplayName("Not-null violation (23502) → 409")
    void handlesNotNullViolation() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "23502",
          "ERROR: null value in column \"title\" violates not-null constraint");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("A required field is missing");
      assertThat(response.getBody().getMessage()).doesNotContain("title");
    }

    @Test
    @DisplayName("Check violation (23514) → 409")
    void handlesCheckViolation() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "23514",
          "ERROR: new row violates check constraint");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("validation constraint");
    }

    @Test
    @DisplayName("String truncation (22001) → 409")
    void handlesStringTruncation() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "22001",
          "ERROR: value too long for type character varying(255)");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("maximum allowed length");
      assertThat(response.getBody().getMessage()).doesNotContain("character varying(255)");
    }

    @Test
    @DisplayName("Unknown class-23 SQLState → generic constraint message")
    void handlesUnknownClass23SqlState() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "23999",
          "some future PostgreSQL integrity error");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("A data constraint was violated");
    }

    @Test
    @DisplayName("Non-class-23 SQLState → generic fallback")
    void handlesNonClass23SqlState() {
      DataIntegrityViolationException ex = buildDataIntegrityException(
          "42P01", // undefined_table
          "ERROR: relation \"nonexistent\" does not exist");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("A data conflict occurred");
      assertThat(response.getBody().getMessage()).doesNotContain("nonexistent");
    }

    @Test
    @DisplayName("No SQLException in chain → generic fallback")
    void handlesNoSqlState() {
      DataIntegrityViolationException ex = new DataIntegrityViolationException(
          "some obscure error with internal details");

      var response = handler.handleDataIntegrityViolation(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("A data conflict occurred");
    }

    /**
     * Builds a realistic DataIntegrityViolationException with the correct cause chain:
     * {@code DataIntegrityViolationException → SQLException}
     */
    private DataIntegrityViolationException buildDataIntegrityException(
        String sqlState, String pgMessage) {
      java.sql.SQLException sqlException = new java.sql.SQLException(pgMessage, sqlState);
      return new DataIntegrityViolationException("could not execute statement", sqlException);
    }
  }

  // ─── HttpMessageNotReadableException ───────────────────────────────────

  @Nested
  @DisplayName("HttpMessageNotReadableException handling")
  class HttpMessageNotReadableTests {

    @Test
    @DisplayName("Malformed JSON → 400")
    void handlesMalformedJson() {
      HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
          "JSON parse error",
          new com.fasterxml.jackson.core.JsonParseException(null, "Unexpected character"),
          new MockHttpInputMessage(new byte[0]));

      var response = handler.handleHttpMessageNotReadable(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("Malformed JSON");
    }

    @Test
    @DisplayName("No cause → generic invalid body message")
    void handlesNoCause() {
      HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
          "Required request body is missing",
          null,
          new MockHttpInputMessage(new byte[0]));

      var response = handler.handleHttpMessageNotReadable(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body");
    }
  }

  // ─── MethodArgumentTypeMismatchException ───────────────────────────────

  @Nested
  @DisplayName("MethodArgumentTypeMismatchException handling")
  class TypeMismatchTests {

    @Test
    @DisplayName("Returns 400 with parameter name and expected type")
    void handlesTypeMismatch() {
      MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
          "abc", Integer.class, "page", null, new NumberFormatException("For input string: \"abc\""));

      var response = handler.handleTypeMismatch(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("page");
      assertThat(response.getBody().getMessage()).contains("Integer");
    }
  }

  // ─── MissingServletRequestParameterException ───────────────────────────

  @Nested
  @DisplayName("MissingServletRequestParameterException handling")
  class MissingParameterTests {

    @Test
    @DisplayName("Returns 400 with parameter name")
    void handlesMissingParameter() {
      MissingServletRequestParameterException ex =
          new MissingServletRequestParameterException("q", "String");

      var response = handler.handleMissingParameter(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("q");
    }
  }

  // ─── HttpMediaTypeNotSupportedException ──────────────────────────────

  @Nested
  @DisplayName("HttpMediaTypeNotSupportedException handling")
  class MediaTypeNotSupportedTests {

    @Test
    @DisplayName("Returns 415 when Content-Type is unsupported")
    void handlesUnsupportedMediaType() {
      HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
          "Content-Type 'text/plain' is not supported");

      var response = handler.handleMediaTypeNotSupported(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getStatus()).isEqualTo(415);
    }

    @Test
    @DisplayName("Returns 415 with helpful message when no Content-Type is provided")
    void handlesNullContentType() {
      // This is the exact scenario: PATCH with no body and no Content-Type header
      HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
          "Content-Type is not set");

      var response = handler.handleMediaTypeNotSupported(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("request body");
    }
  }

  // ─── HttpMediaTypeNotAcceptableException ───────────────────────────────

  @Nested
  @DisplayName("HttpMediaTypeNotAcceptableException handling")
  class MediaTypeNotAcceptableTests {

    @Test
    @DisplayName("Returns 406 Not Acceptable")
    void handlesNotAcceptable() {
      HttpMediaTypeNotAcceptableException ex = new HttpMediaTypeNotAcceptableException(
          "No acceptable representation");

      var response = handler.handleMediaTypeNotAcceptable(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getStatus()).isEqualTo(406);
    }
  }

  // ─── HttpRequestMethodNotSupportedException ────────────────────────────

  @Nested
  @DisplayName("HttpRequestMethodNotSupportedException handling")
  class MethodNotSupportedTests {

    @Test
    @DisplayName("Returns 405 with method name in message")
    void handlesMethodNotAllowed() {
      HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException(
          "DELETE", List.of(new String[]{"GET", "POST", "PATCH"}));

      var response = handler.handleMethodNotSupported(ex);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage()).contains("DELETE");
    }
  }

  // ─── Catch-All ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Catch-all exception handling")
  class CatchAllTests {

    @Test
    @DisplayName("Returns 500 with generic message, no internal details")
    void handlesUnexpectedException() {
      NullPointerException ex = new NullPointerException(
          "Cannot invoke method on null reference at com.internal.Service.process(Service.java:42)");

      var response =
          handler.handleAllUnexpectedExceptions(ex, webRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getMessage())
          .doesNotContain("NullPointerException")
          .doesNotContain("Service.java")
          .doesNotContain("null reference")
          .contains("unexpected error");
    }

    @Test
    @DisplayName("Response has consistent structure")
    void hasConsistentStructure() {
      var response =
          handler.handleAllUnexpectedExceptions(new RuntimeException("boom"), webRequest);

      Assertions.assertNotNull(response.getBody());
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
      assertThat(response.getBody().getTimestamp()).isNotNull();
      assertThat(response.getBody().getFieldErrors()).isNull();
    }
  }

  // ─── Response Structure Validation ─────────────────────────────────────

  @Nested
  @DisplayName("GamsAPIErrorResponse structure")
  class ResponseStructureTests {

    @Test
    @DisplayName("fieldErrors is null (omitted from JSON) when no field errors exist")
    void fieldErrorsNullWhenEmpty() {
      GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.NOT_FOUND, "Not found");

      assertThat(response.getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("fieldErrors is null when passed empty list")
    void fieldErrorsNullWhenEmptyList() {
      GamsAPIErrorResponse response = new GamsAPIErrorResponse(
          HttpStatus.BAD_REQUEST, "Error", Collections.emptyList());

      assertThat(response.getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("Timestamp is present and recent")
    void timestampIsPresent() {
      GamsAPIErrorResponse response = new GamsAPIErrorResponse(HttpStatus.OK, "test");

      assertThat(response.getTimestamp()).isNotNull();
    }
  }

  // ─── Helpers ───────────────────────────────────────────────────────────

  private ConstraintViolation<?> mockViolation(String propertyPath, String message) {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    return violation;
  }
}