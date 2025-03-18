package org.zim.gamsapi.Ingest.utils.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitContentFile;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;

public class BagitContentFileDatastreamConverterTest extends UnitTest {


  BagitContentFile bagitContentFile;
  BagitContentFileDatastreamConverter converter = new BagitContentFileDatastreamConverter();

  @BeforeEach
  public void setUp(){
    bagitContentFile = new BagitContentFile();
    bagitContentFile.setDsid("dsid");
    bagitContentFile.setMimetype("mimetype");
    bagitContentFile.setSize(1L);
    bagitContentFile.setTitle("title");
    bagitContentFile.setCreator("creator");
    bagitContentFile.setDescription("description");
    bagitContentFile.setRights("rights");
  }


  @Test
  public void convertsExpectedBagitContentFileToDatastream() {

    bagitContentFile.setTags(TestDatastream.DATASTREAM_TAGS);
    Datastream datastream = converter.convert(bagitContentFile);
    Assertions.assertNotNull(datastream);
    Assertions.assertEquals(bagitContentFile.getDsid(), datastream.getDsid());
    Assertions.assertEquals(bagitContentFile.getMimetype(), datastream.getMimeType());
    Assertions.assertEquals(bagitContentFile.getSize(), datastream.getSize());
    Assertions.assertEquals(bagitContentFile.getTitle(), datastream.getBaseMetadata().getTitle());
    Assertions.assertEquals(bagitContentFile.getCreator(), datastream.getBaseMetadata().getCreator());
    Assertions.assertEquals(bagitContentFile.getDescription(), datastream.getBaseMetadata().getDescription());
    Assertions.assertEquals(bagitContentFile.getRights(), datastream.getBaseMetadata().getRights());
    Assertions.assertEquals(bagitContentFile.getTags(), datastream.getTags());


  }

  @Test
  public void convertedBagContainsExpectedTags(){
    Datastream datastream = converter.convert(bagitContentFile);
    Assertions.assertEquals(bagitContentFile.getTags(), datastream.getTags());
  }

}
