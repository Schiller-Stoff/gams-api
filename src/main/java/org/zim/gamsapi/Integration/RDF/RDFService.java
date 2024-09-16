package org.zim.gamsapi.Integration.RDF;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.Integration.RDF.utils.JenaFusekiClient;
import org.zim.gamsapi.Integration.RDF.utils.RDFSearchProperties;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RDFService implements IIntegrationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IDatastreamContentRepository datastreamContentRepository;
  private final JenaFusekiClient tripleStoreClient;

  @Override
  public void indexObjects(String projectAbbr) {
    digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr).forEach(digitalObject -> {
      indexObjectDefaultRdf(digitalObject);
      indexObjectCustomRdf(digitalObject);
    });
  }

  @Override
  public void deleteIndexedObjects(String projectAbbr) {

    // delete every subject belonging to a project.
    digitalObjectRepository.findAll().forEach(digitalObject -> {
      String deleteQuery = String.format("DROP GRAPH <%s/%s>",RDFSearchProperties.GAMS_BASE_URL.name, digitalObject.getId());
      try {
        tripleStoreClient.postSPARQL(projectAbbr, deleteQuery);
        String msg = String.format("Successfully deleted object indices for %s for project %s", digitalObject.getId(), projectAbbr);
        log.info(msg);
      } catch (IOException e) {
        String msg = String.format("Failed to delete object indices for %s for project %s", digitalObject.getId(), projectAbbr);
        log.error(msg);
      }
    });

  }

  public void indexObject(String projectAbbr, String id){

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new IntegrationDataProcessingException(String.format("Digital object with pid %s not found", id)));
    log.trace("*** FUSEKI Indexing now object: {}", digitalObject.getId());

    // list of reports for follow-up operations


    // 01. Post default triples.
    indexObjectDefaultRdf(digitalObject);
    // 02. Post custom triples.
    indexObjectCustomRdf(digitalObject);
  }

  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {

    String deleteQuery = String.format("DROP GRAPH <%s/%s>",RDFSearchProperties.GAMS_BASE_URL.name, id);
    try {
      tripleStoreClient.postSPARQL(id, deleteQuery);
      String msg = String.format("Successfully deleted object indices %s for project %s", id, projectAbbr);
      log.trace(msg);
    } catch (IOException e){
      String msg = String.format("Failed to delete object indices for %s for project %s. Original error: %s", projectAbbr, id, e);
      log.trace(msg);
    }
  }


  /**
   * Sends default RDF to the triplestore, like statements about being a digital object having a pid.
   * @param digitalObject object to be indexed.
   */
  private void indexObjectDefaultRdf(DigitalObject digitalObject){
    Set<Datastream> datastreams =  datastreamRepository.findAllByDigitalObject(digitalObject);
    String turtle = tripleStoreClient.buildDefaultIndexingTriple(digitalObject, datastreams);
    try {
      tripleStoreClient.postNQuads(digitalObject, turtle);
      String msg = String.format("Successfully created default indices for digital object %s for project %s", digitalObject.getId(), digitalObject.getProject().getProjectAbbr());
      log.info(msg);
    } catch (IOException e){
      String msg = String.format("Failed to creat default indices for digital object %s for project %s", digitalObject.getId(), digitalObject.getProject().getProjectAbbr());
      log.error(msg);
    }

  }


  /**
   * Checks if the required datastream is available and sends to the triplestore.
   * @param digitalObject Origin of the rdf datastream.
   */
  private void indexObjectCustomRdf(DigitalObject digitalObject){

    var datastreams = datastreamRepository.findAllDatastreamIdViewsByDigitalObject(digitalObject);

    // Load datastream "RDF_TTL" and send to jena-fuseki
    datastreams
        .stream()
        .filter(datastream -> datastream.getDsid().equals("RDF_TTL"))
        .forEach(datastream -> {
              DatastreamId datastreamId = DatastreamId.builder().dsid(datastream.getDsid()).digitalObject(digitalObject.getId()).build();
              InputStreamResource inputStreamResource =  datastreamContentRepository.findById(datastreamId);
              try {
                // parse given RDF first
                Model rdfModel = RDFParser.create()
                    .lang(RDFLanguages.TURTLE)
                    .source(inputStreamResource.getInputStream())
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
                log.info(msg);
              } catch (IOException e) {
                String msg = String.format("Failed to send custom rdf datastream to triplestore. For object %s and project %s. Original error: %s", digitalObject.getId(),digitalObject.getProject().getProjectAbbr(), e);
                log.error(msg);
              }
            });

  }

}
