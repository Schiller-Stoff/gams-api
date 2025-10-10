package org.zim.gamsapi.TestUtilities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamContent.DatastreamContentDeletionFailure;
import org.zim.gamsapi.Datastream.DatastreamContent.DatastreamContentRepository;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.EventCaptureListener;
import org.zim.gamsapi.GAMSCollection.GAMSCollection;
import org.zim.gamsapi.GAMSCollection.IGAMSCollectionRepository;
import org.zim.gamsapi.DigitalObject.Ingest.interfaces.IIngestRecordRepository;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

@Service
@Transactional
@Slf4j
public class TestCleanupService {

  private final DatastreamContentRepository datastreamContentRepository;
  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private EventCaptureListener eventCaptureListener;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDublinCoreEntryRepository dublinCoreElementRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;
  @Autowired
  private IProjectRepository projectRepository;
  @Autowired
  private IGAMSCollectionRepository collectionRepostory;
  @Autowired
  private IIngestRecordRepository bagEntityRepository;

  public TestCleanupService(DatastreamContentRepository datastreamContentRepository) {
    this.datastreamContentRepository = datastreamContentRepository;
  }

  /**
   * Main method cleans up all test data using the repository layer of the
   * gams-api.
   */
  @Transactional
  public void cleanup(){
    eventCaptureListener.clearEvents();
    datastreamContentRepository.deleteAll();
    dublinCoreElementRepository.deleteAll();
    datastreamRepository.deleteAll();
    collectionRepostory.deleteAll();
    bagEntityRepository.deleteAll();
    digitalObjectRepository.deleteAll();
    projectRepository.deleteAll();
  }

  /**
   * Clean up using EntityManager native queries - no custom repository methods needed
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanupAllTestDataViaNativeQueries() {

    eventCaptureListener.clearEvents();

    // delete all data from the filesystem
    datastreamContentRepository.deleteAll();

    try {
      log.debug("Starting test data cleanup using EntityManager");

      // Disable foreign key checks for PostgresSQL
      entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();

      // Delete in any order since FK checks are disabled
      executeDeleteAllQuery(DatastreamContentDeletionFailure.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(DublinCoreEntry.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(GAMSCollection.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(Datastream.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(DigitalObject.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(Project.ORDERED_MANAGED_TABLES);

      // Re-enable foreign key checks
      entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();

      entityManager.flush();
      entityManager.clear();

      log.debug("Test data cleanup completed successfully");

    } catch (Exception e) {
      log.error("Error during test data cleanup", e);
      // Always try to re-enable FK checks
      try {
        entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
      } catch (Exception ignored) {}
      throw e;
    }
  }

  /**
   * Cleans up all test data in a safe manner using ordered managed tables.
   * This method ensures that all deletions are performed in the correct order
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanupAllTestDataSafeViaNativeQueries() {

    // clear all events
    eventCaptureListener.clearEvents();

    // delete all data from the filesystem
    datastreamContentRepository.deleteAll();

    try {
      log.debug("Starting safe test data cleanup");
      executeDeleteAllQuery(DatastreamContentDeletionFailure.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(DublinCoreEntry.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(GAMSCollection.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(Datastream.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(DigitalObject.ORDERED_MANAGED_TABLES);
      executeDeleteAllQuery(Project.ORDERED_MANAGED_TABLES);

      entityManager.flush();
      entityManager.clear();

    } catch (Exception e) {
      log.error("Error during safe cleanup", e);
      throw e;
    }
  }

  /**
   * Execute delete all queries for all managed tables in the given order
   * @param orderedTableNames Array of table names in the order they should be deleted
   */
  private void executeDeleteAllQuery(String[] orderedTableNames) {
    for (String tableName : orderedTableNames) {
      executeDeleteAllQuery(tableName);
    }
  }

  /**
   * Execute a delete all query for a specific table
   * @param tableName Name of the table to delete all rows from
   */
  private void executeDeleteAllQuery(String tableName) {

    final String DELETE_ALL_QUERY = String.format("DELETE FROM %s", tableName);

    try {
      int deletedRows = entityManager.createNativeQuery(DELETE_ALL_QUERY).executeUpdate();
      if (deletedRows > 0) {
        log.trace("Deleted {} rows from table: {}", deletedRows, tableName);
      }
    } catch (Exception e) {
      log.warn("Failed to delete from table: {} - Error: {}", tableName, e.getMessage());
      throw e;
    }
  }

}


