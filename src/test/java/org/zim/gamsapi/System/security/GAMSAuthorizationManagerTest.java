package org.zim.gamsapi.System.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;
import org.zim.gamsapi.System.security.exceptions.UserAuthenticationRequiredException;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;
import org.zim.gamsapi.UnitTest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


public class GAMSAuthorizationManagerTest extends UnitTest {

  @Mock
  Authentication authentication;

  @Mock
  RequestAuthorizationContext authorizationContext;

  @Mock
  HttpServletRequest request;


  @BeforeEach
  public void setUp() {
    // makes sure that mock request is returned
    when(authorizationContext.getRequest()).thenReturn(request);
  }

  /**
   * All test cases where the authorization is being skipped - like: when only HEAD requests.
   */
  @Nested
  public class SkipAuthorizationConditions {

    @Test
    public void headRequestsAreAuthorizedInAnyCase() {

      when(request.getMethod()).thenReturn(HttpMethod.HEAD.name());

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();

      // Act
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());
    }

    @Test
    public void GETRequestsAreAuthorizedInAnyCase() {

      when(request.getMethod()).thenReturn(HttpMethod.GET.name());

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();

      // Act
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());
    }

    @Test
    public void throwsUserAuthenticationRequiredExceptionIfNotAuthenticatedForPOST() {
      // Arrange
      when(authentication.isAuthenticated()).thenReturn(false);
      when(request.getMethod()).thenReturn(HttpMethod.POST.name());

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();

      Assertions.assertThrows(UserAuthenticationRequiredException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });

    }

  }

  @Nested
  public class RolesAndAuthorities {

    @BeforeEach
    public void setUp(){
      when(authentication.isAuthenticated()).thenReturn(true);
      when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    }

    @Test
    public void userWithAnonymousRoleIsNotAuthorizedForPOST() {

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPISecurityRoles.getAnonymous())
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      // verifies that the method was called
      Mockito.verify(authentication).getAuthorities();
      assertTrue(!decision.isGranted());
    }

    @Test
    public void globalAdminIsAuthorizedForPOST() {

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPISecurityRoles.getAdmin())
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // make sure that this was actually called
      Mockito.verify(authentication).getAuthorities();
      assertTrue(decision.isGranted());
    }


    @Test
    public void throwsIfProjectAbbrIsNotSetInRequest() {
      // return empty map for variables (projectAbbr)
      when(authorizationContext.getVariables()).thenReturn(Map.of());

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();

      // Act
      Assertions.assertThrows(AuthorizationConfigurationException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });

      // verify that related method was actually called
      Mockito.verify(authorizationContext).getVariables();
    }

    @Test
    public void throwsIfStandardUserMissesRequiredProjectRole(){

      final String PROJECT_ABBR = "test";
      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPISecurityRoles.getProjectAdmin("otherProject"))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      GAMSAuthorizationManager manager = new GAMSAuthorizationManager();

      // Act
      Assertions.assertThrows(UserNotAssignedToProjectException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });

      Mockito.verify(authorizationContext).getVariables();

    }


  }


  @Test
  public void checkTest() {

    // mock user is authenticated
    //when(authentication.isAuthenticated()).thenReturn(true);

    // TODO externally applied project variable in request?
    // TODO is this a good idea or should it be done in a different way?
    when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", "test"));

    // mock user roles
    List<GrantedAuthority> testAuthorities = List.of(
        // TODO this role just doesn't exist!
        new SimpleGrantedAuthority("ROLE_USER")
    );

    // TODO
    when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

    // mock the request
    when(authorizationContext.getRequest()).thenReturn(request);
    when(request.getMethod()).thenReturn("POST");

    GAMSAuthorizationManager manager = new GAMSAuthorizationManager();

    // Act
    AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

    // Assert
    assertTrue(decision.isGranted());
  }

}
