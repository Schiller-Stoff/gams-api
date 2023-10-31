package org.zim.gamsapi.Integration;

import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Generic controller interface for all integrated databases / services in gams-integration-api.
 */
public interface IIntegrationController {

  List<IntegrationActionReport> indexProjectObjects(@PathVariable String projectAbbr);

  IntegrationActionReport deleteProjectObjects(@PathVariable String projectAbbr);

  List<IntegrationActionReport> indexObject(@PathVariable String projectAbbr, @PathVariable String pid);

  IntegrationActionReport deleteObject(@PathVariable String projectAbbr, @PathVariable String pid);

}
