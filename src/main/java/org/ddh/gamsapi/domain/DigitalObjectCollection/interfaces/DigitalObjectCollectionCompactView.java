package org.ddh.gamsapi.domain.DigitalObjectCollection.interfaces;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectIdView;
import org.ddh.gamsapi.domain.Project.interfaces.ProjectIdView;

import java.util.Set;

@JacksonXmlRootElement(localName = "digitalObject")
public interface DigitalObjectCollectionCompactView {

  String getId();

  String getTitle();

  String getDescription();

  ProjectIdView getProject();

  Set<DigitalObjectIdView> getDigitalObjects();

}
