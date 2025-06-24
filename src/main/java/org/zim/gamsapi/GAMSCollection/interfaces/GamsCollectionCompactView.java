package org.zim.gamsapi.GAMSCollection.interfaces;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.Project.interfaces.ProjectIdView;

import java.util.Set;

@JacksonXmlRootElement(localName = "digitalObject")
public interface GamsCollectionCompactView {

  String getId();

  String getTitle();

  String getDescription();

  ProjectIdView getProject();

  Set<DigitalObjectIdView> getDigitalObjects();

}
