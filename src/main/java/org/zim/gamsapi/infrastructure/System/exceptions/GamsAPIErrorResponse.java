package org.zim.gamsapi.infrastructure.System.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global error response for GAMS API mainly used in global exception handling.
 */
@Getter
public class GamsAPIErrorResponse {

  private HttpStatus status;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime timestamp;
  private String message;
  private Map<String, String> errors;

  public GamsAPIErrorResponse(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
    this.timestamp = LocalDateTime.now();
    this.errors = new HashMap<>();
  }

  public GamsAPIErrorResponse(HttpStatus status, String message, Map<String, String> errors) {
    this(status, message);
    this.errors = errors;
  }
}