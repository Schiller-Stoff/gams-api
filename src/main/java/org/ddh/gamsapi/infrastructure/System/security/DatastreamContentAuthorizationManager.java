package org.ddh.gamsapi.infrastructure.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatastreamContentAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  private final IDatastreamRepository datastreamRepository;
  private final DatastreamAuthorizationService datastreamAuthorizationService;

  @Override
  public AuthorizationDecision authorize(
      Supplier<? extends @Nullable Authentication> authenticationSupplier,
      RequestAuthorizationContext context
  ) {
    String projectAbbr = context.getVariables().get("projectAbbr");
    String digitalObjectId = context.getVariables().get("id");
    String dsid = context.getVariables().get("dsid");


    // 1. Load only content restrictions (lightweight)
    Set<String> contentRestrictions =
        datastreamRepository.findContentRestrictionsByDigitalObjectIdAndDsid(
            digitalObjectId, dsid);

    // 2. Delegate to shared authorization logic
    return datastreamAuthorizationService.checkContentAccess(
        projectAbbr,
        contentRestrictions,
        authenticationSupplier.get()
    );
  }
}