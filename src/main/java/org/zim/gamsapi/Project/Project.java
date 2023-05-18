package org.zim.gamsapi.Project;

import jakarta.validation.constraints.NotBlank;
import jdk.dynalink.linker.LinkerServices;
import lombok.Data;
import org.zim.gamsapi.DigitalObject.DigitalObject;

import java.util.List;

/**
 * Class representing a GAMS project.
 */
@Data
public class Project {

  @NotBlank
  private final String projectAbbr;

}
