package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(value = { "/api/v1/projects/{projectAbbr}/objects/{id}/archival-records" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class ArchivalRecordController {

  private final IArchivalRecordService archivalRecordService;
  private final IProjectService projectService;

  @Operation(
      summary = "Get archival records",
      description = "Retrieve the archival records associated with a specific digital object within a project.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the archival records",
              content = @Content)
      }
  )
  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  public List<ArchivalRecordCompactView> findSubmissionRecord(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return archivalRecordService.findForObject(id);
  }


}
