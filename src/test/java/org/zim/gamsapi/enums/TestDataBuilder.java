package org.zim.gamsapi.enums;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.DatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.GAMSCollection.IGAMSCollectionRepository;
import org.zim.gamsapi.Project.Project;
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

  @Autowired
  private IGAMSCollectionRepository gamsCollectionRepository;

  @Transactional
  public void removeAllExceptProjects(TestDataSet testDataSet) {
    datastreamContentRepository.delete(testDataSet.mainDatastream().deriveDatastreamId());
    dublinCoreEntryRepository.delete(testDataSet.dublinCoreEntry());
    datastreamRepository.delete(testDataSet.mainDatastream());
    gamsCollectionRepository.delete(testDataSet.gamsCollection());
    digitalObjectRepository.delete(testDataSet.digitalObject());
  }

  /**
   * Adds a random project to the already existing data in the database.
   * @param testDataSet the test data set to which the project will be added
   * @return the saved project
   */
  @Transactional
  public Project addRandomProject(TestDataSet testDataSet) {

    // create a random project id
    // id must not be longer than 10 characters
    var randomProjectId = TestProject.PROJECT_ABBR.getValue() + System.currentTimeMillis();
    if (randomProjectId.length() > 10) {
      randomProjectId = randomProjectId.substring(0, 10);
    }

    // check if the id already exists
    if (projectRepository.existsById(randomProjectId)) {
      String msg = String.format("Trying to save random project but calculated a duplicated id: %s", randomProjectId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    var projectToBeSaved = ProjectBuilder.builder()
        .projectAbbr(randomProjectId)
        .description(TestProject.PROJECT_DESCRIPTION.getValue())
        .title(TestProject.PROJECT_TITLE.getValue())
        .build();

    return projectRepository.save(projectToBeSaved);
  }

  /**
   * Adds a random datastream to the already existing data in the database.
   * @param testDataSet the test data set to which the datastream will be added
   * @return the saved datastream
   */
  @Transactional
  public Datastream addRandomDatastream(TestDataSet testDataSet) {

    // create a random datastream id
    var randomDatastreamId = System.currentTimeMillis() + TestDatastream.DSID.getValue();

    // check if the id already exists
    var existingIds = datastreamRepository.findAllByDigitalObjectId(testDataSet.digitalObject().getId());
    if (existingIds.stream().anyMatch(datastream -> datastream.getDsid().equals(randomDatastreamId))) {
      String msg = String.format("Trying to save random datastream but calculated duplicated ids: %s", randomDatastreamId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    var datastreamToBeSaved = DatastreamBuilder.builder()
        .dsid(randomDatastreamId)
        .digitalObject(testDataSet.digitalObject())
        .tags(TestDatastream.DATASTREAM_TAGS)
        .baseMetadata(TestDatastream.METADATA_BASE_ENTITY)
        .size( (long) TestDatastreamContent.CONTENT.getValue().length())
        .mimeType(TestDatastream.MIME_TYPE.getValue())
        .fileName(TestDatastream.FILE_NAME.getValue())
        .lang(TestDatastream.DATASTREAM_LANG)
        .build();

    return datastreamRepository.save(datastreamToBeSaved);
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
  public void removeTestCollection(TestDataSet testDataSet) {
    gamsCollectionRepository.delete(testDataSet.gamsCollection());
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

    // save the content of the datastream
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

    var GAMSCollection = TestGAMSCollection.generate(
        persistedProject.getProjectAbbr(),
        persistedDigitalObject.getId(),
        persistedDatastream.getDsid()
    );

    var persistedGAMSCollection =
        gamsCollectionRepository.save(GAMSCollection);

    return new TestDataSet(
        persistedProject,
        persistedDigitalObject,
        persistedDatastream,
        persistedDublinCoreEntry,
        persistedGAMSCollection
    );
  }


}
