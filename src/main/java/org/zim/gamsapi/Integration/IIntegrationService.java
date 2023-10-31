package org.zim.gamsapi.Integration;

import java.util.List;

/**
 * Describes the indexing of DigitalObjects and specific datastreams
 * to integrated services, like apache solr or jena-fuseki.
 */
public interface IIntegrationService {


  /**
   * Indexes digital objects of given project
   */
  List<IntegrationActionReport> indexObjects(String projectAbbr);


  /**
   * Deletes all data dependent on the digital objects, like direct
   * representations of the object AND derived entities, like persons, etc.
   */
  IntegrationActionReport deleteIndexedObjects(String projectAbbr);

  /**
   * Indexes a single object
   * @param projectAbbr project to be indexed
   * @param id id of the object to be indexed
   * @return IndexingReport
   */
  List<IntegrationActionReport> indexObject(String projectAbbr, String id);

  /**
   * Deletes a single object from the a database alongside dependent entities, like persons etc.
   * @param projectAbbr project to be indexed to facets database
   * @param id id of the object to be deleted
   * @return IndexingReport
   */
  IntegrationActionReport deleteIndexedObject(String projectAbbr, String id);

}
