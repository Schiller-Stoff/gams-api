package org.zim.gamsapi.Ingest.interfaces;

import org.zim.gamsapi.Ingest.Ingest;

public interface IIngestService {

  /**
   * Handles ingest of one singular digital object with contained datastreams.
   * @param ingest Submission Information Package to be ingested
   */
  void ingest(Ingest ingest);

}
