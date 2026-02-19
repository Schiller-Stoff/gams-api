package org.ddh.gamsapi.infrastructure.System.security;

import jakarta.servlet.http.HttpServletRequest;
import org.ddh.gamsapi.infrastructure.System.configproperties.GamsEnvironmentProperties;
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
import org.ddh.gamsapi.infrastructure.System.security.exceptions.AuthorizationConfigurationException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAssignedToProjectException;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;
import org.ddh.gamsapi.UnitTest;
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

  UserProjectAuthorizationManager manager;


  @BeforeEach
  public void setUp() {
    var gamsEnvironmentProperties = new GamsEnvironmentProperties();
    gamsEnvironmentProperties.setAllowDirectModifications(true);
    manager =  new UserProjectAuthorizationManager(gamsEnvironmentProperties);

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

      // Act
      AuthorizationDecision decision = manager.authorize(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());
    }

    @Test
    public void GETRequestsAreAuthorizedInAnyCase() {

      when(request.getMethod()).thenReturn(HttpMethod.GET.name());

      // Act
      AuthorizationDecision decision = manager.authorize(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());
    }

    @Test
    public void throwsAccessDeniedExceptionIfNotAuthenticatedForPOST() {
      // Arrange
      when(authentication.isAuthenticated()).thenReturn(false);
      when(request.getMethod()).thenReturn(HttpMethod.POST.name());

      Assertions.assertThrows(AccessDeniedException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
      });

    }

    @Test
    public void throwsAuthorizationConfigurationExceptionWhenNoProjectAbbrInRequest() {
      when(request.getMethod()).thenReturn(HttpMethod.GET.name());
      when(request.getRequestURI()).thenReturn("/noProjectAbbr");

      Assertions.assertThrows(AuthorizationConfigurationException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
      });
    }

    @Test
    public void doesNotThrowWhenProjectAbbrInRequest() {
      when(request.getMethod()).thenReturn(HttpMethod.GET.name());
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test");

      Assertions.assertDoesNotThrow(() -> {
        manager.authorize(() -> authentication, authorizationContext);
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

      // Assert
      Assertions.assertThrows(UserNotAuthorizedException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
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

      AuthorizationDecision decision = manager.authorize(() -> authentication, authorizationContext);

      // make sure that this was actually called
      Mockito.verify(authentication).getAuthorities();
      assertTrue(decision.isGranted());
    }


    @Test
    public void throwsIfProjectAbbrIsNotSetInRequest() {
      // return empty map for variables (projectAbbr)
      when(authorizationContext.getVariables()).thenReturn(Map.of());

      // Act
      Assertions.assertThrows(AuthorizationConfigurationException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
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

      // Act
      Assertions.assertThrows(UserNotAssignedToProjectException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
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

      // Act
      AuthorizationDecision decision = manager.authorize(() -> authentication, authorizationContext);

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

      // Act
      AuthorizationDecision decision = manager.authorize(() -> authentication, authorizationContext);

      // Assert
      assertTrue(decision.isGranted());

    }

    @Test
    public void projectEditorIsStillAuthorizedForPOSTIfAllowDirectModificationIsFalse() {

      var properties = new GamsEnvironmentProperties();
      properties.setAllowDirectModifications(false);
      manager = new UserProjectAuthorizationManager(properties);

      final String PROJECT_ABBR = "test";

      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);


      AuthorizationDecision decision = manager.authorize(() -> authentication, authorizationContext);
      // Assert
      assertTrue(decision.isGranted());

    }
  }

  @Nested
  public class AllowDirectModificationsFalseRoleModelIncrement {

    @BeforeEach
    public void setUp(){
      when(authentication.isAuthenticated()).thenReturn(true);
      when(request.getRemoteUser()).thenReturn("test");
      when(request.getSession()).thenReturn(new MockHttpSession());

      // setting allowDirectModifications to false
      var gamsEnvironmentProperties = new GamsEnvironmentProperties();
      gamsEnvironmentProperties.setAllowDirectModifications(false);
      manager =  new UserProjectAuthorizationManager(gamsEnvironmentProperties);
    }

    @Test
    public void denyProjectEditorPATCHObject(){
      // mocking a valid request uri
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test");
      when(request.getMethod()).thenReturn(HttpMethod.PATCH.name());

      final String PROJECT_ABBR = "test";
      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      // Act
      Assertions.assertThrows(UserNotAuthorizedException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
      });
    }

    @Test
    public void stillAllowProjectEditorDELETEObject(){
      // mocking a valid request uri
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test");
      when(request.getMethod()).thenReturn(HttpMethod.DELETE.name());

      final String PROJECT_ABBR = "test";
      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      // Act
      var decision = manager.authorize(() -> authentication, authorizationContext);
      Assertions.assertTrue(decision.isGranted());
    }

    @Test
    public void stillAllowProjectEditorINGESTObject(){
      // mocking a valid request uri
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects");
      when(request.getMethod()).thenReturn(HttpMethod.POST.name());

      final String PROJECT_ABBR = "test";
      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      // Act
      var decision = manager.authorize(() -> authentication, authorizationContext);
      Assertions.assertTrue(decision.isGranted());
    }

    @Test
    public void denyProjectEditorDeleteDatastream(){
      // mocking a valid request uri
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test/datastreams/text.xml");
      when(request.getMethod()).thenReturn(HttpMethod.DELETE.name());

      final String PROJECT_ABBR = "test";
      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      var gamsEnvironmentProperties = new GamsEnvironmentProperties();
      gamsEnvironmentProperties.setAllowDirectModifications(false);
      manager =  new UserProjectAuthorizationManager(gamsEnvironmentProperties);

      // Act
      Assertions.assertThrows(UserNotAuthorizedException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
      });
    }

    @Test
    public void denyProjectEditorPUTDatastream(){
      // mocking a valid request uri
      when(request.getRequestURI()).thenReturn("/api/v1/projects/test/objects/test/datastreams/text.xml");
      when(request.getMethod()).thenReturn(HttpMethod.PUT.name());

      final String PROJECT_ABBR = "test";
      when(authorizationContext.getVariables()).thenReturn(Map.of("projectAbbr", PROJECT_ABBR));

      // Arrange
      List<GrantedAuthority> testAuthorities = List.of(
          new SimpleGrantedAuthority(GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR))
      );
      when(authentication.getAuthorities()).thenReturn((List) testAuthorities);

      var gamsEnvironmentProperties = new GamsEnvironmentProperties();
      gamsEnvironmentProperties.setAllowDirectModifications(false);
      manager =  new UserProjectAuthorizationManager(gamsEnvironmentProperties);

      // Act
      Assertions.assertThrows(UserNotAuthorizedException.class, () -> {
        manager.authorize(() -> authentication, authorizationContext);
      });
    }
  }
}
