package org.ddh.gamsapi.infrastructure.User;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.ddh.gamsapi.IntegrationTest;

@AutoConfigureMockMvc
public class UserControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  final String USER_INFO_ENDPOINT = "/api/v1/userinfo";


  @Test
  public void webclientContainsTestUsername_whenLoggedIn() throws Exception {

    final String OIDC_TEST_USERNAME_HTML = "<h2 class=\"mb-3\">user</h2>";

    MvcResult mvcResult = mockMvc.perform(
        MockMvcRequestBuilders
            .get(USER_INFO_ENDPOINT)
            .with(SecurityMockMvcRequestPostProcessors.oidcLogin())
      )
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andReturn();


    Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(OIDC_TEST_USERNAME_HTML);

  }

  @Test
  public void webclientRedirectsToLogin_whenNotLoggedIn() throws Exception {

    mockMvc.perform(
        MockMvcRequestBuilders
            .get(USER_INFO_ENDPOINT)
      )
      .andExpect(MockMvcResultMatchers.status().is3xxRedirection());

  }


}
