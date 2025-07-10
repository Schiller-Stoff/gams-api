package org.zim.gamsapi.enums;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.DatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

/**
 * TestDataBuilder is a component that builds test data sets for testing.
 */
@Component
@Slf4j
public class TestDataBuilder {

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;

  @Autowired
  private IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Autowired
  private DatastreamContentRepository datastreamContentRepository;

  @Transactional
  public void removeAllExceptProjects(TestDataSet testDataSet) throws Exception {
    datastreamContentRepository.delete(testDataSet.mainDatastream().deriveDatastreamId());
    dublinCoreEntryRepository.delete(testDataSet.dublinCoreEntry());
    datastreamRepository.delete(testDataSet.mainDatastream());
    digitalObjectRepository.delete(testDataSet.digitalObject());
  }

  /**
   * Adds a random digital object to the already existing data in the database
   *
   * @param testDataSet the test data set to which the digital object will be added
   * @return the saved digital object
   * @throws IllegalStateException if a digital object with the same id already exists
   */
  @Transactional
  public DigitalObject addRandomObject(TestDataSet testDataSet) {

    // create a random digital object id
    var randomDigitalObjectId = TestDigitalObject.DIGITAL_OBJECT_ID.getValue() + System.currentTimeMillis();

    // check if the id already exists
    var existingIds = digitalObjectRepository.findAllByProject_ProjectAbbr(testDataSet.project().getProjectAbbr());
    if (existingIds.stream().anyMatch(digitalObject -> digitalObject.getId().equals(randomDigitalObjectId))) {
      String msg = String.format("Trying to save random digital object but calculated duplicated ids: %s", randomDigitalObjectId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    var digitalObjectToBeSaved = DigitalObjectBuilder.builder()
        .id(randomDigitalObjectId)
        .project(testDataSet.project())
        .publisher(TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue())
        .objectType(TestDigitalObject.DIGITAL_OBJECT_TYPE.getValue())
        .funder(TestDigitalObject.DIGITAL_OBJECT_FUNDER.getValue())
        .mainResource(TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
        .baseMetadata(TestMetadataBaseEntity.generate())
        .build();

    return digitalObjectRepository.save(digitalObjectToBeSaved);
  }

  @Transactional
  public TestDataSet buildTestDataSet() {

    var projectToBeSaved = ProjectBuilder.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .description(TestProject.PROJECT_DESCRIPTION.getValue())
        .title(TestProject.PROJECT_TITLE.getValue())
        // following fields are supplied by the database / spring security worflows
        //.createdBy(TestUser.USERNAME.getValue())
        //.modifiedBy(TestUser.USERNAME.getValue())
        //.created(new Date())
        //.modified(new Date())
        //.published(new Date())
        .build();

    var persistedProject = projectRepository.save(projectToBeSaved);

    var digitalObjectToBeSaved = DigitalObjectBuilder.builder()
        .id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        .project(persistedProject)
        .publisher(TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue())
        .objectType(TestDigitalObject.DIGITAL_OBJECT_TYPE.getValue())
        .funder(TestDigitalObject.DIGITAL_OBJECT_FUNDER.getValue())
        .mainResource(TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
        .baseMetadata(TestMetadataBaseEntity.generate())
        .build();

    var persistedDigitalObject = digitalObjectRepository.save(digitalObjectToBeSaved);

    var datastreamToBeSaved = DatastreamBuilder.builder()
        .dsid(TestDatastream.DSID.getValue())
        .digitalObject(persistedDigitalObject)
        .tags(TestDatastream.DATASTREAM_TAGS)
        .baseMetadata(TestDatastream.METADATA_BASE_ENTITY)
        .size( (long) TestDatastreamContent.CONTENT.getValue().length())
        .mimeType(TestDatastream.MIME_TYPE.getValue())
        .fileName(TestDatastream.FILE_NAME.getValue())
        .lang(TestDatastream.DATASTREAM_LANG)
        .build();

    var persistedDatastream = datastreamRepository.save(datastreamToBeSaved);

    // TODO Save the datastream content?
    datastreamContentRepository.save(
        TestDatastreamContent.CONTENT.getValue().getBytes()
        , persistedDatastream.deriveDatastreamId()
    );

    var dublinCoreEntryToBeSaved = DublinCoreEntry.builder()
        .name(TestDublinCoreEntry.NAME.getValue())
        .value(TestDublinCoreEntry.VALUE.getValue())
        .language(TestDublinCoreEntry.LANGUAGE.getValue())
        .digitalObject(persistedDigitalObject).build();

    var persistedDublinCoreEntry = dublinCoreEntryRepository.save(dublinCoreEntryToBeSaved);

    return new TestDataSet(persistedProject, persistedDigitalObject, persistedDatastream, persistedDublinCoreEntry);
  }


}
