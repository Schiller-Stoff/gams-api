package org.zim.gamsapi.Integration.Common.interfaces;

import org.springframework.web.bind.annotation.PathVariable;
import org.zim.gamsapi.Integration.Common.IntegrationActionReport;

import java.util.List;

/**
 * Generic controller interface for all integrated databases / services in gams-integration-api.
 */
public interface IIntegrationController {

  List<IntegrationActionReport> indexProjectObjects(@PathVariable String projectAbbr);

  List<IntegrationActionReport> deleteProjectObjects(@PathVariable String projectAbbr);

  List<IntegrationActionReport> indexObject(@PathVariable String projectAbbr, @PathVariable String pid);

  List<IntegrationActionReport> deleteObject(@PathVariable String projectAbbr, @PathVariable String pid);

}
