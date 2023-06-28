package org.zim.gamsapi.System;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.zim.gamsapi.User.User;
import org.zim.gamsapi.User.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping({"/api/v1/", "/api/v1"})
@RequiredArgsConstructor
@Controller
public class ManagementController {

  private final IUserService userService;

  @GetMapping
  public String getIndexPage(Model model){
    // user model is added globally via controller advice
    return "index";
  }

  @GetMapping({"projects", "projects/"})
  public String showAllProjects(HttpServletRequest httpServletRequest, Model model){
    User user = userService.getCurrentUser(httpServletRequest);
    model.addAttribute(user);
    return "Project/show_all";
  }

  @GetMapping(value = {"projects", "projects/"}, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public User showAllProjectsJson(HttpServletRequest request){
    User user = userService.getCurrentUser(request);
    return user;
  }

}
