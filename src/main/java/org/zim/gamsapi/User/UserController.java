package org.zim.gamsapi.User;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zim.gamsapi.User.interfaces.IUserService;

@Controller
@RequestMapping("/api/v1/user")
public class UserController {

  public IUserService userService;

  public UserController(IUserService userService) {
    this.userService = userService;
  }


  @GetMapping(path = "/{username}")
  public String showUserProjects(User user, Model model){
    user = userService.findByUsername(user.getUsername());
    model.addAttribute(user);
    return "User/userprojects";
  }

  @GetMapping
  public String showUserProjectsViaCredentials(HttpServletRequest request,User user, Model model){
    String authUsername = request.getRemoteUser();
    // TODO error handling here
    user = userService.findByUsername(authUsername);
    model.addAttribute(user);
    return "User/userprojects";
  }

  @PostMapping
  public String createUser(User user, Model model){
    user = userService.saveUser(user);
    model.addAttribute(user);
    return "User/userprojects";
  }

}
