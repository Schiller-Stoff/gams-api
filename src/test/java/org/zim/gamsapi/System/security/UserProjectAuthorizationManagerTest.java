package org.zim.gamsapi.System.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;
import org.zim.gamsapi.System.security.exceptions.UserNotAuthorizedException;
import org.zim.gamsapi.UnitTest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


public class UserProjectAuthorizationManagerTest extends UnitTest {

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
    // mockig a valid request uri
    when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test");
  }

  /**
   * All test cases where the authorization is being skipped - like: when only HEAD requests.
   */
  @Nested
  public class SkipAuthorizationConditions {

    @Test
    public void headRequestsAreAuthorizedInAnyCase() {

      when(request.getMethod()).thenReturn(HttpMethod.HEAD.name());

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      // Act
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());
    }

    @Test
    public void GETRequestsAreAuthorizedInAnyCase() {

      when(request.getMethod()).thenReturn(HttpMethod.GET.name());

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      // Act
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());
    }

    @Test
    public void throwsAccessDeniedExceptionIfNotAuthenticatedForPOST() {
      // Arrange
      when(authentication.isAuthenticated()).thenReturn(false);
      when(request.getMethod()).thenReturn(HttpMethod.POST.name());

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      Assertions.assertThrows(AccessDeniedException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });

    }

    @Test
    public void throwsAuthorizationConfigurationExceptionWhenNoProjectAbbrInRequest() {
      when(request.getMethod()).thenReturn(HttpMethod.GET.name());
      when(request.getRequestURI()).thenReturn("/noProjectAbbr");

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      Assertions.assertThrows(AuthorizationConfigurationException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });
    }

    @Test
    public void doesNotThrowWhenProjectAbbrInRequest() {
      when(request.getMethod()).thenReturn(HttpMethod.GET.name());
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test");

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      Assertions.assertDoesNotThrow(() -> {
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
      when(request.getRemoteUser()).thenReturn("test");
      when(request.getSession()).thenReturn(new MockHttpSession());
    }

    @Test
    public void userWithAnonymousRoleIsNotAuthorizedForPOST() {

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getAnonymous())
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      // Assert
      Assertions.assertThrows(UserNotAuthorizedException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });

      Mockito.verify(authentication).getAuthorities();
    }

    @Test
    public void globalAdminIsAuthorizedForPOST() {

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getAdmin())
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // make sure that this was actually called
      Mockito.verify(authentication).getAuthorities();
      assertTrue(decision.isGranted());
    }


    @Test
    public void throwsIfProjectAbbrIsNotSetInRequest() {
      // return empty map for variables (projectAbbr)
      when(authorizationContext.getVariables()).thenReturn(Map.of());

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

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
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin("otherProject"))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      // Act
      Assertions.assertThrows(UserNotAssignedToProjectException.class, () -> {
        manager.check(() -> authentication, authorizationContext);
      });

      Mockito.verify(authorizationContext).getVariables();

    }

    @Test
    public void projectAdminIsAuthorizedForPOST() {

      final String PROJECT_ABBR = "test";

      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectAdmin(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      // Act
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());

    }

    @Test
    public void projectEditorIsAuthorizedForPOST() {
      final String PROJECT_ABBR = "test";

      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      UserProjectAuthorizationManager manager = new UserProjectAuthorizationManager();

      // Act
      AuthorizationDecision decision = manager.check(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());

    }
  }
}
