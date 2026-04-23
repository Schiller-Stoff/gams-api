package org.ddh.gamsapi.infrastructure.System.security;

import lombok.Getter;
import org.springframework.security.authorization.AuthorizationDecision;

/**
 * Custom authorization decision that contains a client reason (for users to understand why they are being blocked from the api)
 */
@Getter
public class GamsApiAuthorizationDecision extends AuthorizationDecision {

  private String clientReason;

  public GamsApiAuthorizationDecision(boolean granted) {
    super(granted);
  }

  public GamsApiAuthorizationDecision(boolean granted, String clientReason) {
    super(granted);
    this.clientReason = clientReason;
  }
}
