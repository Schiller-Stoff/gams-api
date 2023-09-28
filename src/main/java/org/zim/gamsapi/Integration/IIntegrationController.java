package org.zim.gamsapi.Integration;

import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Generic controller interface for all integrated databases / services in gams-integration-api.
 */
public interface IIntegrationController {

  List<IndexingReport> indexProjectObjects(@PathVariable String projectAbbr);

  IndexingReport deleteProjectObjects(@PathVariable String projectAbbr);

  List<IndexingReport> indexObject(@PathVariable String projectAbbr, @PathVariable String pid);

  IndexingReport deleteObject(@PathVariable String projectAbbr, @PathVariable String pid);

}
