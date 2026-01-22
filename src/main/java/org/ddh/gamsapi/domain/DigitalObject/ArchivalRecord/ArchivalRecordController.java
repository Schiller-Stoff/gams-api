package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = { "/api/v1/projects/{projectAbbr}/objects/{id}/archival-records" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class ArchivalRecordController {

  private final IArchivalRecordService archivalRecordService;
  private final IProjectService projectService;


}
