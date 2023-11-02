package org.zim.gamsapi.Integration.RDF;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.*;
import org.apache.jena.sparql.core.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Integration.Common.IntegrationActionReport;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionStatus;
import org.zim.gamsapi.Integration.Common.enums.IntegrationActionType;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.exceptions.ProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.Integration.RDF.utils.JenaFusekiClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.zim.gamsapi.Integration.RDF.utils.RDFSearchProperties;
@Service
@Slf4j
@RequiredArgsConstructor
public class RDFService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final JenaFusekiClient tripleStoreClient;

  @Override
  public List<IntegrationActionReport> indexObjects(String projectAbbr) {

    List<IntegrationActionReport> integrationActionReports = new ArrayList<>();

    digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr).forEach(digitalObject -> {
      IntegrationActionReport defaultIndexReport = indexObjectDefaultRdf(digitalObject);
      integrationActionReports.add(defaultIndexReport);
      IntegrationActionReport customIndexReport = indexObjectCustomRdf(digitalObject);
      integrationActionReports.add(customIndexReport);
    });

    return integrationActionReports;
  }

  @Override
  public List<IntegrationActionReport> deleteIndexedObjects(String projectAbbr) {

    List<IntegrationActionReport> integrationActionReports = new ArrayList<>();

    // delete every subject belonging to a project.
    digitalObjectRepository.findAll().forEach(digitalObject -> {
      String deleteQuery = String.format("DROP GRAPH <%s/%s>",RDFSearchProperties.GAMS_BASE_URL.name, digitalObject.getId());
      try {
        tripleStoreClient.postSPARQL(projectAbbr, deleteQuery);
        String msg = String.format("Successfully deleted object indices for %s for project %s", digitalObject.getId(), projectAbbr);
        integrationActionReports.add(
          new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.SUCCESS, msg)
        );
      } catch (IOException e) {
        String msg = String.format("Failed to delete object indices for %s for project %s", digitalObject.getId(), projectAbbr);
        integrationActionReports.add(
                new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.ERROR, msg)
        );
      }
    });

    return integrationActionReports;
  }

  public List<IntegrationActionReport> indexObject(String projectAbbr, String id){

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new ProcessingException(String.format("Digital object with pid %s not found", id)));
    log.trace("*** FUSEKI Indexing now object: {}", digitalObject.getId());

    // list of reports for follow-up operations
    List<IntegrationActionReport> integrationActionReports = new ArrayList<>();

    // 01. Post custom indexing triples.
    IntegrationActionReport indexObjectDefaultRdfReport = indexObjectDefaultRdf(digitalObject);
    integrationActionReports.add(indexObjectDefaultRdfReport);
    // 02. Load datastream "RDF_TTL" and send to jena-fuseki
    IntegrationActionReport indexCustomRdfReport = indexObjectCustomRdf(digitalObject);
    integrationActionReports.add(indexCustomRdfReport);
    return integrationActionReports;
  }

  @Override
  public List<IntegrationActionReport> deleteIndexedObject(String projectAbbr, String id) {

    List<IntegrationActionReport> integrationActionReports = new ArrayList<>();
    String deleteQuery = String.format("DROP GRAPH <%s/%s>",RDFSearchProperties.GAMS_BASE_URL.name, id);

    try {
      tripleStoreClient.postSPARQL(id, deleteQuery);
      String msg = String.format("Successfully deleted object indices %s for project %s", id, projectAbbr);
      log.trace(msg);
      integrationActionReports.add(
        new IntegrationActionReport(projectAbbr,IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.SUCCESS, msg)
      );
    } catch (IOException e){
      String msg = String.format("Failed to delete object indices for %s for project %s. Original error: %s", projectAbbr, id, e);
      log.trace(msg);
      integrationActionReports.add(
        new IntegrationActionReport(projectAbbr, IntegrationActionType.DELETE_OBJECT, IntegrationActionStatus.ERROR, msg)
      );
    }

    return integrationActionReports;
  }


  /**
   * Sends default RDF to the triplestore, like statements about being a digital object having a pid.
   * @param digitalObject object to be indexed.
   */
  private IntegrationActionReport indexObjectDefaultRdf(DigitalObject digitalObject){
    String turtle = tripleStoreClient.buildDefaultIndexingTriple(digitalObject);
    try {
      tripleStoreClient.postNQuads(digitalObject, turtle);
      String msg = String.format("Successfully created default indices for digital object %s for project %s", digitalObject.getId(), digitalObject.getProject().getProjectAbbr());
      return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SUCCESS, msg);
    } catch (IOException e){
      String msg = String.format("Failed to creat default indices for digital object %s for project %s", digitalObject.getId(), digitalObject.getProject().getProjectAbbr());
      return new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.ERROR, msg);
    }

  }


  /**
   * Checks if the required datastream is available and sends to the triplestore.
   * @param digitalObject Origin of the rdf datastream.
   */
  private IntegrationActionReport indexObjectCustomRdf(DigitalObject digitalObject){
    // some values are being set later --> by default operation is being skipped
    String defaultMsg = String.format("Skipped indexing custom RDF of object %s for project %s because no custom rdf datastream was found.", digitalObject.getId(), digitalObject.getProject().getProjectAbbr());
    IntegrationActionReport integrationActionReport = new IntegrationActionReport(digitalObject.getProject().getProjectAbbr(), IntegrationActionType.INDEX_OBJECT, IntegrationActionStatus.SKIPPED, defaultMsg);

    // Load datastream "RDF_TTL" and send to jena-fuseki
    digitalObject.getDatastreams()
            .stream()
            .filter(datastream -> datastream.getDsid().toLowerCase().equals(GAMSAPIntegrationDatastreamId.RDF_DATASTREAM_ID.name))
            .forEach(datastream -> {
              Resource datastreamData = new ByteArrayResource(datastream.getData());
              try {
                // parse given RDF first
                Model rdfModel = RDFParser.create()
                    .lang(RDFLanguages.TURTLE)
                    .source(datastreamData.getInputStream())
                    .base(RDFSearchProperties.GAMS_BASE_URL.name)
                    .toModel();

                // create quad statements assigning the named graph of the project.
                DatasetGraph newDatasetGraph = DatasetGraphFactory.create();
                rdfModel.listStatements().forEach(statement -> {
                  Property namedGraphStmt = rdfModel.createProperty( RDFSearchProperties.GAMS_BASE_URL.name +  "/" + digitalObject.getId());
                  Quad quad = Quad.create(namedGraphStmt.asNode(),statement.asTriple());
                  newDatasetGraph.add(quad);
                });

                String quads = RDFWriter.source(newDatasetGraph).lang(Lang.NQUADS).asString();
                tripleStoreClient.postNQuads(digitalObject,quads);
                String msg = String.format("Successfully indexed custom object RDF for object %s , for project: %s", digitalObject.getId(), digitalObject.getProject().getProjectAbbr());
                integrationActionReport.setMessage(msg);
                integrationActionReport.setStatus(IntegrationActionStatus.SUCCESS);
              } catch (IOException e) {
                String msg = String.format("Failed to send custom rdf datastream to triplestore. For object %s and project %s. Original error: %s", digitalObject.getId(),digitalObject.getProject().getProjectAbbr(), e);
                log.error(msg);
                integrationActionReport.setMessage(msg);
                integrationActionReport.setStatus(IntegrationActionStatus.ERROR);
              }
            });

        return integrationActionReport;

  }

}
