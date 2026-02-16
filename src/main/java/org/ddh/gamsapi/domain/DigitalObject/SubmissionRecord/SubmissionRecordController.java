package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;

@Controller
@RequestMapping(value = { "/api/v1/projects/{projectAbbr}/objects/{id}/submission-record" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class SubmissionRecordController {


  private final ISubmissionRecordService submissionRecordService;
  private final IProjectService projectService;

  @Operation(
      summary = "Get Submission Record",
      description = "Retrieve the submission record associated with a specific digital object within a project.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successful retrieval of the submission record",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Submission record not found",
              content = @Content)
      }
  )
  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  public SubmissionRecord findSubmissionRecord(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return submissionRecordService.find(id).orElseThrow(() -> new SubmissionRecordNotFoundException("Submission record not found for digital object with ID: " + id));
  }

}
