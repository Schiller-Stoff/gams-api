package org.zim.gamsapi.Integration.Common.interfaces;

import org.springframework.web.bind.annotation.PathVariable;

/**
 * Generic controller interface for all integrated databases / services in gams-integration-api.
 */
public interface IIntegrationController {

  void indexProjectObjects(@PathVariable String projectAbbr);

  void deleteProjectObjects(@PathVariable String projectAbbr);

  void indexObject(@PathVariable String projectAbbr, @PathVariable String pid);

  void deleteObject(@PathVariable String projectAbbr, @PathVariable String pid);

}
