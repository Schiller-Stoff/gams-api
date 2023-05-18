package org.zim.gamsapi.SubInfoPack.interfaces;

import org.zim.gamsapi.SubInfoPack.SubInfoPack;

public interface ISubInfoPackService {

  /**
   * Handles ingest of one singular digital object with contained datastreams.
   * @param subInfoPack Submission Information Package to be ingested
   */
  void ingest(SubInfoPack subInfoPack);

}
