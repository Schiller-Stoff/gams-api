package org.ddh.gamsapi.application.Integration.SemanticSearch;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationController;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class SemanticSearchController implements IIntegrationController {

  public static final String SEMANTIC_SEARCH_GET_PATH = "/api/v1/integration/semantic-search";

  public static final String SEMANTIC_SEARCH_MANAGEMENT_PATH = SEMANTIC_SEARCH_GET_PATH + "/projects/{projectAbbr}/objects";

  public static final String CUSTOM_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH = SEMANTIC_SEARCH_MANAGEMENT_PATH + "/{id}";

  @Override
  public void indexProjectObjects(String projectAbbr) {

  }

  @Override
  public void deleteProjectObjects(String projectAbbr) {

  }

  @Override
  public void indexObject(String projectAbbr, String pid) {

  }

  @Override
  public void deleteObject(String projectAbbr, String pid) {

  }
}
