package org.zim.gamsapi.application.Ingest;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zim.gamsapi.application.Ingest.Ingest;
import org.zim.gamsapi.application.Ingest.IngestController;
import org.zim.gamsapi.application.Ingest.exceptions.IngestProcessingException;
import org.zim.gamsapi.application.Ingest.interfaces.IIngestService;
import org.zim.gamsapi.application.Ingest.utils.IngestStatics;
import org.zim.gamsapi.domain.Project.interfaces.IProjectService;
import org.zim.gamsapi.UnitTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class IngestControllerTest extends UnitTest {

  @Test
  public void throwsIfRequiredRequestPartIsNull() throws Exception {
    // Arrange
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getPart(IngestStatics.FORM_PART_NAME.name)).thenReturn(null);

    IngestController controller = new IngestController(Mockito.mock(IIngestService.class), Mockito.mock(IProjectService.class));

    // Act and Assert
    assertThrows(IngestProcessingException.class, () -> {
      controller.ingest(new Ingest(), request);
    });
  }
}