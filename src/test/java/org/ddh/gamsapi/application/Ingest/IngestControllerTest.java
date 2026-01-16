package org.ddh.gamsapi.application.Ingest;

import jakarta.servlet.http.HttpServletRequest;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.IngestStatics;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.UnitTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class IngestControllerTest extends UnitTest {

  @Test
  @Disabled("Disabled until we can mock HttpServletRequest.getPart properly")
  public void throwsIfRequiredRequestPartIsNull() throws Exception {
    // Arrange
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getPart(IngestStatics.FORM_PART_NAME.name)).thenReturn(null);

    IngestController controller = new IngestController(Mockito.mock(IIngestService.class), Mockito.mock(IProjectService.class));

    // Act and Assert
    assertThrows(IngestProcessingException.class, () -> {
      //controller.ingest(TestProject.PROJECT_ABBR.getValue(), request);
    });
  }
}