package org.ddh.gamsapi.application.WebDeployment;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.WebDeployment.dto.WebDeploymentInfo;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentNotFoundException;
import org.ddh.gamsapi.application.WebDeployment.exceptions.WebDeploymentValidationException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Controller
@RequestMapping("/api/v1/projects/{projectAbbr}/web")
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.PROJECTS_TAG, description = OpenAPIConfig.PROJECTS_TAG_DESCRIPTION)
public class WebDeploymentController {

  private final WebDeploymentService webDeploymentService;
  private final IProjectService projectService;

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Deploy a static web site for a project",
      description = "Uploads a zip archive containing the static site output "
          + "(e.g., from the pollin-tool) and deploys it for the given project. "
          + "Each PUT fully replaces the previous deployment. "
          + "The static files are served by nginx, not by this API.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Deployment successful",
              content = @Content(schema = @Schema(
                  implementation = WebDeploymentInfo.class))),
          @ApiResponse(responseCode = "400",
              description = "Invalid or empty zip archive",
              content = @Content),
          @ApiResponse(responseCode = "404",
              description = "Project not found",
              content = @Content)
      }
  )
  @Parameter(name = "projectAbbr",
      description = "Project abbreviation", required = true)
  public WebDeploymentInfo deploy(
      @PathVariable String projectAbbr,
      @RequestParam("file") MultipartFile file
  ) {
    if (file.isEmpty()) {
      throw new WebDeploymentValidationException(
          "Uploaded file is empty");
    }

    try (InputStream inputStream = file.getInputStream()) {
      return webDeploymentService.deploy(projectAbbr, inputStream);
    } catch (IOException e) {
      throw new WebDeploymentValidationException(
          "Failed to read uploaded file: " + e.getMessage());
    }
  }

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Get deployment metadata for a project",
      description = "Returns metadata about the current web deployment "
          + "including deployment timestamp, file count, and total size. "
          + "Does NOT return the static files themselves.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Deployment info returned",
              content = @Content(schema = @Schema(
                  implementation = WebDeploymentInfo.class))),
          @ApiResponse(responseCode = "404",
              description = "Project or deployment not found",
              content = @Content)
      }
  )
  public WebDeploymentInfo getDeploymentInfo(
      @PathVariable String projectAbbr
  ) {
    return webDeploymentService.getDeploymentInfo(projectAbbr);
  }

  @DeleteMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Remove the deployed static site for a project",
      description = "Deletes the static web deployment for the given project. "
          + "Removes both filesystem content and the database record.",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Deployment removed successfully",
              content = @Content),
          @ApiResponse(responseCode = "404",
              description = "Project or deployment not found",
              content = @Content)
      }
  )
  public void undeploy(@PathVariable String projectAbbr) {
    webDeploymentService.undeploy(projectAbbr);
  }

  // ─── HTML (Thymeleaf) endpoints ──────────────────────────────────────

  @Hidden
  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getDeploymentInfoHtml(
      @PathVariable String projectAbbr,
      Model model,
      Authentication authentication
  ) {
    var project = projectService.findProject(projectAbbr);
    model.addAttribute("project", project);

    // Authentication state for conditional rendering
    boolean isAuthenticated = authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
    model.addAttribute("isAuthenticated", isAuthenticated);

    // Deployment info — may not exist yet
    try {
      WebDeploymentInfo deploymentInfo = webDeploymentService.getDeploymentInfo(projectAbbr);
      model.addAttribute("deployment", deploymentInfo);
    } catch (WebDeploymentNotFoundException _) {
      model.addAttribute("deployment", null);
    }

    return "WebDeployment/show";
  }

  @Hidden
  @DeleteMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String undeployHtml(@PathVariable String projectAbbr) {
    webDeploymentService.undeploy(projectAbbr);
    return "redirect:/api/v1/projects/" + projectAbbr + "/web";
  }
}