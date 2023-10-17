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
import org.zim.gamsapi.Integration.GamsDatastreamIds;
import org.zim.gamsapi.Integration.IIntegrationService;
import org.zim.gamsapi.Integration.IndexingReport;
import org.zim.gamsapi.Integration.ProcessingException;
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
  public List<IndexingReport> indexObjects(String projectAbbr) {
    digitalObjectRepository.findDigitalObjectsByProject_ProjectAbbr(projectAbbr).forEach(digitalObject -> {
      indexObjectDefaultRdf(digitalObject);
      indexObjectCustomRdf(digitalObject);
    });

    // TODO build missing indexing report
    return new ArrayList<>(List.of(new IndexingReport("demo", "nice", "haha")));
  }

  @Override
  public IndexingReport deleteIndexedObjects(String projectAbbr) {

    // ~Outdated deletion based on triples
    // delete every subject belonging to a project.
    //String deleteQuery = String.format("DELETE WHERE { ?subject %s \"%s\". ?subject ?predicate ?object. }", RDFSearchProperties.HAS_PROJECT_ABBR.name, projectAbbr);

    // delete every subject belonging to a project.
    digitalObjectRepository.findAll().forEach(digitalObject -> {
      //TODO use enum
      String deleteQuery = String.format("DROP GRAPH <%s/%s>",RDFSearchProperties.GAMS_BASE_URL.name, digitalObject.getId());
      tripleStoreClient.postSPARQL(projectAbbr, deleteQuery);
    });

    // TODO construct indexing reports
    return new IndexingReport("demo", "nice", "haha");
  }

  public List<IndexingReport> indexObject(String projectAbbr, String id){

    DigitalObject digitalObject = digitalObjectRepository.findById(id)
            .orElseThrow(() -> new ProcessingException(String.format("Digital object with pid %s not found", id)));
    log.trace("*** FUSEKI Indexing now object: {}", digitalObject.getId());
    // 01. Post custom indexing triples.
    indexObjectDefaultRdf(digitalObject);
    // 02. Load datastream "RDF_TTL" and send to jena-fuseki
    indexObjectCustomRdf(digitalObject);
    // TODO construct valid indexing report.
    return new ArrayList<>(List.of(new IndexingReport("demo", "nice", "haha")));
  }

  @Override
  public IndexingReport deleteIndexedObject(String projectAbbr, String id) {
    // OUTDATED delete query based on triples
    // deletes all subjects where gams:hasPid = given pid
    // String deleteQuery = String.format("DELETE WHERE { ?s %s \"%s\". ?s ?p ?o.}", RDFSearchProperties.HAS_PID.name, pid);

    String deleteQuery = String.format("DROP GRAPH <%s/%s>",RDFSearchProperties.GAMS_BASE_URL.name, id);
    tripleStoreClient.postSPARQL(id,deleteQuery);
    // TODO build an indexing report?
    return new IndexingReport("demo", "nice", "haha");
  }


  /**
   * Sends default RDF to the triplestore, like statements about being a digital object having a pid.
   * @param digitalObject object to be indexed.
   */
  private void indexObjectDefaultRdf(DigitalObject digitalObject){
    String turtle = tripleStoreClient.buildDefaultIndexingTriple(digitalObject);
    tripleStoreClient.postNQuads(digitalObject, turtle);
  }


  /**
   * Checks if the required datastream is available and sends to the triplestore.
   * @param digitalObject Origin of the rdf datastream.
   */
  private void indexObjectCustomRdf(DigitalObject digitalObject){

    // 02. Load datastream "RDF_TTL" and send to jena-fuseki
    digitalObject.getDatastreams()
            .stream()
            .filter(datastream -> datastream.getDsid().toLowerCase().equals(GamsDatastreamIds.RDF_DATASTREAM_ID.name))
            .forEach(datastream -> {
              Resource datastreamData = new ByteArrayResource(datastream.getData());
              try {
                // parse given RDF first
                Model rdfModel = RDFParser.create()
                    .lang(RDFLanguages.TURTLE)
                    .source(datastreamData.getInputStream())
                    .base(RDFSearchProperties.GAMS_BASE_URL.name)
                    .toModel();

                // TODO think about: would be necessary to delete based on pid / project abbreviation aside from named graphs.
                // TODO can then formulate SPARQL DELETE queries based on this properties (risk of inconsistency)
//                Property hasProjectAbbr = rdfModel.createProperty("https://gams.uni-graz.at/ontology#hasProjectAbbr");
//                Property hasPid = rdfModel.createProperty("https://gams.uni-graz.at/ontology#hasPid");
//                rdfModel.listSubjects().forEach(subject -> {
//                  subject.addProperty(hasProjectAbbr, digitalObject.getProjectAbbr());
//                  subject.addProperty(hasPid, digitalObject.getPid());
//                });

                // create quad statements assigning the named graph of the project.
                DatasetGraph newDatasetGraph = DatasetGraphFactory.create();
                rdfModel.listStatements().forEach(statement -> {
                  Property namedGraphStmt = rdfModel.createProperty( RDFSearchProperties.GAMS_BASE_URL.name +  "/" + digitalObject.getId());
                  Quad quad = Quad.create(namedGraphStmt.asNode(),statement.asTriple());
                  newDatasetGraph.add(quad);
                });

                String quads = RDFWriter.source(newDatasetGraph).lang(Lang.NQUADS).asString();
                tripleStoreClient.postNQuads(digitalObject,quads);
              } catch (IOException e) {
                String msg = String.format("Failed to send rdf datastream to triplestore. For object %s. Original error: %s", digitalObject.getId(), e);
                log.error(msg);
                throw new ProcessingException(msg);
              }
            });
  }

}
