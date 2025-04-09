package org.zim.gamsapi.System.exceptions;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    GamsAPIErrorResponse gamsAPIErrorResponse = new GamsAPIErrorResponse(HttpStatus.BAD_REQUEST, "Validation error", errors);
    return new ResponseEntity<>(gamsAPIErrorResponse, HttpStatus.BAD_REQUEST);
  }

}