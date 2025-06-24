package org.zim.gamsapi.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.System.config.OpenAPIConfig;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/v1")
@Slf4j
@Tag(name = OpenAPIConfig.USER_TAG, description = OpenAPIConfig.USER_TAG_DESCRIPTION)
public class UserController {

  @Operation(
      summary = "Show user info",
      description = "Displays auth information associated with the authenticated user. If the user is not authenticated, they will be redirected to the login page."
  )
  @GetMapping(path = {"/userinfo"})
  public String showUserProjectsViaCredentials(Model model){

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if(!authentication.isAuthenticated()){
      log.trace("User is not authenticated to see user info - redirecting to login page");
      return "redirect:/api/v1/auth";
    }

    DefaultOidcUser oidcUser;
    try {
      oidcUser = (DefaultOidcUser) authentication.getPrincipal();
    } catch (ClassCastException e){
      log.trace("Failed to cast the authentication principal to DefaultOidcUser. User is not authenticated to see user info - redirecting to login page");
      return "redirect:/api/v1/auth";
    }

    String userName = oidcUser.getSubject();
    if(userName == null){
      log.trace("Failed to get the user name from the authentication principal. User is not authenticated to see user info - redirecting to login page");
      return "redirect:/api/v1/auth";
    }

    User user = User.builder()
        .username(userName)
        .userid(userName)
        .authorities(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
        .build();

    model.addAttribute(user);
    return "User/userprojects";
  }


}
