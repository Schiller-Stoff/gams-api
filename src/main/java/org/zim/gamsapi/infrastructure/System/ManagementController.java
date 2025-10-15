package org.zim.gamsapi.infrastructure.System;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping({"/api/v1/", "/api/v1"})
@RequiredArgsConstructor
@Controller
public class ManagementController {

  @GetMapping
  public String getIndexPage(){
    return "index";
  }


}
