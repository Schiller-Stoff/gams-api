package org.zim.gamsapi.System.exceptions;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler for the GAMS API.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleTransactionSystemExceotion(TransactionSystemException ex) {
    String msg = String.format("TransactionSystemException: %s", ex.getMessage());
    log.error(msg);
    return drillRootErrorCause(ex);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex){
    // TODO implement
    String msg = String.format("DataIntegrityViolationException: %s", ex.getMessage());
    log.error(msg);
    return drillRootErrorCause(ex);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

    String msg = String.format("HttpMessageNotReadableException: Message not readable exception in %s. Original cause:  %s", this.getClass().getName(), ex);
    log.error(msg);

    String errorMessage = "Invalid request body";
    Throwable cause = ex.getCause();

    // Handle different types of JSON errors
    if (cause instanceof JsonParseException) {
      errorMessage = "Malformed JSON request";
    } else if (cause instanceof UnrecognizedPropertyException upe) {
      errorMessage = String.format("Unknown property: '%s'", upe.getPropertyName());
    } else if (cause instanceof InvalidFormatException ife) {
      errorMessage = String.format("Invalid value for property: '%s'",
          ife.getPath().stream()
              .map(JsonMappingException.Reference::getFieldName)
              .filter(Objects::nonNull)
              .collect(Collectors.joining(".")));
    } else if (cause instanceof MismatchedInputException mie) {
      if (!mie.getPath().isEmpty()) {
        errorMessage = String.format("Invalid value type for property: '%s'",
            mie.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(".")));
      }
    }

    GamsAPIErrorResponse gamsAPIErrorResponse = new GamsAPIErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    return new ResponseEntity<>(gamsAPIErrorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GamsAPIErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    GamsAPIErrorResponse gamsAPIErrorResponse = new GamsAPIErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Validation error",
        errors);
    return new ResponseEntity<>(gamsAPIErrorResponse, HttpStatus.BAD_REQUEST);
  }

//  // Handle all other exceptions
//  @ExceptionHandler(Exception.class)
//  protected ResponseEntity<GamsAPIErrorResponse> handleAllExceptions(
//      Exception ex, WebRequest request) {
//    return drillRootErrorCause(ex);
//  }

  /**
   * Drill down the root cause of given error and return a response entity with the error message.
   * @param cause the cause of the error
   * @return a response entity with the error message
   */
  public ResponseEntity<GamsAPIErrorResponse> drillRootErrorCause(Throwable cause){
    String msg;
    ResponseEntity<GamsAPIErrorResponse> responseEntity = null;
    // TODO refactor following code! (make cleaner!)
    while (cause != null) {
      if (cause instanceof ConstraintViolationException) {
        var violations = ((ConstraintViolationException) cause).getConstraintViolations();
        Map<String, String> errors = new HashMap<>();
        for (var violation : violations) {
          String propertyName = violation.getPropertyPath().toString();
          String errMessage = String.format("Property %s | %s | But given was: %s | Validated domain class: %s", violation.getPropertyPath().toString(), violation.getMessage(), violation.getInvalidValue().toString(), violation.getRootBeanClass());
          errors.put(propertyName, errMessage);
        }
        msg = "ConstraintViolationException in the database layer - aborting operations.";
        log.error(msg, cause);
        GamsAPIErrorResponse gamsAPIErrorResponse = new GamsAPIErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, msg, errors);
        responseEntity = new ResponseEntity<>(gamsAPIErrorResponse, gamsAPIErrorResponse.getStatus());
      } else if (cause instanceof DataIntegrityViolationException dataIntegrityViolationException){
        msg = "DataIntegrityViolationException in the database layer";
        log.error(msg, cause);
        var errors = new HashMap<String, String>();
        errors.put("error", dataIntegrityViolationException.getMostSpecificCause().getMessage());
        GamsAPIErrorResponse gamsAPIErrorResponse = new GamsAPIErrorResponse(
            HttpStatus.CONFLICT,
            msg,
            errors
        );
        responseEntity = new ResponseEntity<>(gamsAPIErrorResponse, gamsAPIErrorResponse.getStatus());
      }

      cause = cause.getCause();
    }

    if (responseEntity == null) {
      String errMsg = String.format("Response entity is unexpectedly null - for cause %s", cause);
      log.error(errMsg);
      GamsAPIErrorResponse gamsAPIErrorResponse = new GamsAPIErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errMsg);
      responseEntity = new ResponseEntity<>(gamsAPIErrorResponse, gamsAPIErrorResponse.getStatus());
    }

    return responseEntity;
  }

}