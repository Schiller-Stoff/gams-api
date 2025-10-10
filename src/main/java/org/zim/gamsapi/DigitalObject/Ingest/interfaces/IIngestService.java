package org.zim.gamsapi.DigitalObject.Ingest.interfaces;

import org.zim.gamsapi.DigitalObject.Ingest.Ingest;

public interface IIngestService {

  /**
   * Handles ingest of one singular digital object with contained datastreams.
   * @param ingest Submission Information Package to be ingested
   */
  void ingest(Ingest ingest);

}
