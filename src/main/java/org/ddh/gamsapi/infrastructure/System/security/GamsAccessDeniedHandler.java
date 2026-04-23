package org.ddh.gamsapi.infrastructure.System.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for denied security via authorization process.
 */
@Component
@RequiredArgsConstructor
public class GamsAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest request,
                     HttpServletResponse response,
                     AccessDeniedException ex) throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.TEXT_HTML_VALUE);

    // extract the client / user reason from the authorization decision (my extended custom class)
    String reason = "";
    if (ex instanceof AuthorizationDeniedException ade
        && ade.getAuthorizationResult() instanceof GamsApiAuthorizationDecision gad) {
      reason = gad.getClientReason();
    }
    var body = "<html><h1>" + HttpStatus.FORBIDDEN.value() + "</h1><p>" + reason + "</p></html>";
    response.getOutputStream().write(body.getBytes());
  }
}
