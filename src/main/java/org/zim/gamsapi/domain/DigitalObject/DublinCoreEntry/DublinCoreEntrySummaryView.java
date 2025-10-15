package org.zim.gamsapi.domain.DigitalObject.DublinCoreEntry;

import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectIdView;

/**
 * View for a DublinCoreEntry summary.
 */
public interface DublinCoreEntrySummaryView {

  Long getId();

  String getName();

  String getValue();

  String getLanguage();

  DigitalObjectIdView getDigitalObject();

}
