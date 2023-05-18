package org.zim.gamsapi.User;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zim.gamsapi.User.interfaces.IUserService;

@Controller
@RequestMapping("/api/v1/management/user")
public class UserController {

  public IUserService userService;

  public UserController(IUserService userService) {
    this.userService = userService;
  }

  @GetMapping(path = "/profile")
  public String showUser(HttpServletRequest httpServletRequest, Model model){
    // user model is added globally via controller advice
    // TODO null is okay for demo service
    User user = userService.getCurrentUser(httpServletRequest);
    model.addAttribute(user);
    return "User/profile";
  }

  @GetMapping(path = "/profile", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public User showUserJson(HttpServletRequest request){
    return userService.getCurrentUser(request);
  }

}
