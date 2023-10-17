package org.zim.gamsapi.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zim.gamsapi.User.interfaces.IUserService;

import java.util.List;

@Controller
@RequestMapping("/api/v1")
@Slf4j
public class UserController {

  public IUserService userService;

  public UserController(IUserService userService) {
    this.userService = userService;
  }


  @GetMapping(path = "/user/{username}")
  public String showUserProjects(User user, Model model){
    user = userService.findByUsername(user.getUsername());
    model.addAttribute(user);
    return "User/userprojects";
  }

  @GetMapping(path = "/user")
  public String showUserProjectsViaCredentials(HttpServletRequest request,User user, Model model){
    String authUsername = request.getRemoteUser();
    // TODO error handling here
    user = userService.findByUsername(authUsername);
    model.addAttribute(user);
    return "User/userprojects";
  }

  @PostMapping(path = "/user")
  public String createUser(User user, Model model){
    user = userService.saveUser(user);
    model.addAttribute(user);
    return "User/userprojects";
  }

  @GetMapping(path = "/users")
  @ResponseBody
  public List<User> showAllUsers(){
    return userService.findAll();
  }

}
