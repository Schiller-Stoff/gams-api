package org.ddh.gamsapi.domain.DigitalObjectCollection.interfaces;

import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectDetailsView;
import org.ddh.gamsapi.domain.Project.interfaces.ProjectIdView;

import java.util.Set;

public interface DigitalObjectCollectionDetailsView {

  String getId();

  String getTitle();

  String getDescription();

  ProjectIdView getProject();

  Set<DigitalObjectDetailsView> getDigitalObjects();

}
