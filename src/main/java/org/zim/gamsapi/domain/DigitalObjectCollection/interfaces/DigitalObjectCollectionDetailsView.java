package org.zim.gamsapi.domain.DigitalObjectCollection.interfaces;

import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.domain.Project.interfaces.ProjectIdView;

import java.util.Set;

public interface DigitalObjectCollectionDetailsView {

  String getId();

  String getTitle();

  String getDescription();

  ProjectIdView getProject();

  Set<DigitalObjectDetailsView> getDigitalObjects();

}
