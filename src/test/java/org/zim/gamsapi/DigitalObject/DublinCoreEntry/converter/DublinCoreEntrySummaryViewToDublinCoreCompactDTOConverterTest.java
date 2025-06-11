package org.zim.gamsapi.DigitalObject.DublinCoreEntry.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntryCompactDTO;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectIdView;
import org.zim.gamsapi.UnitTest;

public class DublinCoreEntrySummaryViewToDublinCoreCompactDTOConverterTest extends UnitTest {

  @Test
  public void convertsToExpectedDublinCoreCompactDTO() {

    // first create a test object that implements the DublinCoreEntrySummaryView interface
    DublinCoreEntrySummaryView testObjectSummaryView = new DublinCoreEntrySummaryView() {
      @Override
      public Long getId() {
        return 0L;
      }

      @Override
      public String getName() {
        return "Test";
      }

      @Override
      public String getValue() {
        return "Test Value";
      }

      @Override
      public String getLanguage() {
        return "en";
      }

      @Override
      public DigitalObjectIdView getDigitalObject() {
        return null;
      }
    };

    DublinCoreEntryCompactDTO dublinCoreCompactDTO = new DublinCoreEntrySummaryViewToDublinCoreCompactDTOConverter().convert(testObjectSummaryView);

    Assertions.assertThat(dublinCoreCompactDTO)
        .isNotNull()
        .extracting(DublinCoreEntryCompactDTO::value, DublinCoreEntryCompactDTO::language)
        .containsExactly(testObjectSummaryView.getValue(), testObjectSummaryView.getLanguage());


  }

}
