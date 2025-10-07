package org.zim.gamsapi.GAMSCollection.interfaces;

import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.Project.interfaces.ProjectIdView;

import java.util.Set;

public interface GAMSCollectionDetailsView {

  String getId();

  String getTitle();

  String getDescription();

  ProjectIdView getProject();

  Set<DigitalObjectDetailsView> getDigitalObjects();

}
