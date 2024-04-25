package org.zim.gamsapi.Ingest.interfaces;

import org.zim.gamsapi.Ingest.SubInfoPack;

public interface ISubInfoPackService {

  /**
   * Handles ingest of one singular digital object with contained datastreams.
   * @param subInfoPack Submission Information Package to be ingested
   */
  void ingest(SubInfoPack subInfoPack);

}
