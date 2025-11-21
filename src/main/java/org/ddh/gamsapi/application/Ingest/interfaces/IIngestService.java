package org.ddh.gamsapi.application.Ingest.interfaces;

import org.springframework.transaction.annotation.Transactional;
import org.ddh.gamsapi.application.Ingest.Ingest;

import java.io.InputStream;
import java.io.OutputStream;

public interface IIngestService {

  /**
   * Handles ingest from a zipped BagIt input stream.
   * @param projectAbbr project abbreviation
   * @param bagZipStream input stream of zipped bag (will be closed by caller)
   */
  void ingest(String projectAbbr, InputStream bagZipStream);

  /**
   * Export a digital object as a zipped BagIt package and write it to the provided OutputStream.
   * @param objectId the id of the digital object to export
   * @param outputStream the OutputStream to write the zipped BagIt package to
   */
  @Transactional
  void exportAsBag(String objectId, OutputStream outputStream);

}
