package org.zim.gamsapi.Integration.CoreSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.zim.gamsapi.Datastream.DatastreamContentRepository;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.exceptions.DatastreamCannotLoadFileException;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.Integration.Common.enums.GAMSAPIntegrationDatastreamId;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import org.zim.gamsapi.Integration.Common.utils.XMLUtils;
import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoreSearchService implements IIntegrationService {

  private final CoreSearchRepository digitalObjectElasticRepository;

  private final IDigitalObjectRepository digitalObjectRepository;
  private final DatastreamContentRepository datastreamContentRepository;

  public void indexObject(String projectId, String id) {
    log.info("Adding digital object to elasticsearch with ID: {}", id);

    // Fetch the digital object from the repository
    var digitalObject = digitalObjectRepository.findById(id)
        .orElseThrow(() -> {
          String msg = String.format("Aborting indexing object for the core search service. Could not find digital object with ID: %s. For project %s", id, projectId);
          log.error(msg);
          return new DigitalObjectNotFoundException(msg);
        });

    // to be indexed in elastic search
    CoreSearchEntity coreSearchEntity = new CoreSearchEntity();

    // TODO load dublin core - map to CoreSearchEntity?
    final var DC_DATASTREAM_ID = DatastreamId.builder()
        .digitalObject(id)
        // TODO I'm very unsure because of case sensitivity
        .dsid(GAMSAPIntegrationDatastreamId.DUBLIN_CORE_DATASTREAM_ID.name)
        .build();

    var xmlContent =  datastreamContentRepository.findById(DC_DATASTREAM_ID);

    Document dcXml;
    try {
      dcXml = XMLUtils.parseXml(xmlContent.getInputStream());
    } catch (IOException e) {
      String msg = String.format("Failed to read datastream content %s for datastream %s. Original error: %s", xmlContent.getDescription(), DC_DATASTREAM_ID, e);
      log.error(msg);
      throw new DatastreamCannotLoadFileException(msg);
    }

    // retrieve all child elements of the root element
    // TODO validate if it's correct dublin core?
    // TODO this might be risky (will index all elements in the xml file)
    var dcNodes = XMLUtils.getAllXpath("/*/*", dcXml);

    for (int i = 0; i < dcNodes.getLength(); i++) {
      var node = dcNodes.item(i);
      String nodeName = node.getNodeName();
      String nodeValue = node.getTextContent();
      // read out potential DC lang field
      // TODO rethink assignment of DC lang field
      String optionalDcLang = null;
      try {
        optionalDcLang = XMLUtils.extractAttributeValue("xml:lang", node);
      } catch (IntegrationDataProcessingException e){
        // no lang attribute found
        log.trace("No lang attribute found for dublin core element {}", nodeName);
      }

      // remove "xml:" or "dc:" prefix if it exists
      String nodeNameRemovedPrefix = nodeName.contains(":") ?
          nodeName.substring(nodeName.indexOf(":") + 1) : nodeName;

      switch (nodeNameRemovedPrefix) {
        case "title":
          var dcTitle = CoreSearchEntity.DCElement.builder()
              .name(nodeNameRemovedPrefix)
              .value(nodeValue)
              .lang(optionalDcLang)
              .build();
          coreSearchEntity.addTitle(dcTitle);
          log.debug("Indexing title: {}", nodeValue);
        case "description":
          coreSearchEntity.addDescription(
              CoreSearchEntity.DCElement.builder()
                  .name(nodeNameRemovedPrefix)
                  .value(nodeValue)
                  .lang(optionalDcLang)
                  .build()
          );
          log.debug("Indexing description: {}", nodeValue);
      }

    }

    // then save to elastic search
    coreSearchEntity.setId(digitalObject.getId());
    digitalObjectElasticRepository.save(coreSearchEntity);
  }


  @Override
  public void indexObjects(String projectAbbr) {
    // TODO implement indexing of all digital objects for a project
  }

  @Override
  public void deleteIndexedObjects(String projectAbbr) {
    // TODO implement deletion of all indexed digital objects for a project
  }

  @Override
  public void deleteIndexedObject(String projectAbbr, String id) {
    // TODO implement
  }
}
