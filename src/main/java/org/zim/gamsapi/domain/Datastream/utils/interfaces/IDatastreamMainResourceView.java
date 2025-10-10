package org.zim.gamsapi.domain.Datastream.utils.interfaces;

import org.zim.gamsapi.MetadataBaseEntity;
import java.util.Set;

/**
 * Spring Data JPA Projection Interface for the datastream returned as the main resource of a digital object.
 */
public interface IDatastreamMainResourceView {

  String getDsid();

  DigitalObjectView getDigitalObject();

  String getMimeType();

  MetadataBaseEntity getBaseMetadata();

  Set<String> getTags();

  Set<String> getLang();

  interface DigitalObjectView {
    String getId();
  }
}
