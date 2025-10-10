package org.zim.gamsapi.domain.GAMSCollection.interfaces;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.domain.Project.interfaces.ProjectIdView;

import java.util.Set;

@JacksonXmlRootElement(localName = "digitalObject")
public interface GamsCollectionCompactView {

  String getId();

  String getTitle();

  String getDescription();

  ProjectIdView getProject();

  Set<DigitalObjectIdView> getDigitalObjects();

}
