package org.zim.gamsapi.Ingest.utils.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Ingest.utils.Bagit.BagitContentFile;
import org.zim.gamsapi.UnitTest;

public class BagitContentFileDatastreamConverterTest extends UnitTest {



  @Test
  public void convertsExpectedBagitContentFileToDatastream() {

    BagitContentFile bagitContentFile = new BagitContentFile();
    bagitContentFile.setDsid("dsid");
    bagitContentFile.setMimetype("mimetype");
    bagitContentFile.setSize(1L);
    bagitContentFile.setTitle("title");
    bagitContentFile.setCreator("creator");
    bagitContentFile.setDescription("description");
    bagitContentFile.setPublisher("publisher");
    bagitContentFile.setRights("rights");

    BagitContentFileDatastreamConverter converter = new BagitContentFileDatastreamConverter();
    Datastream datastream = converter.convert(bagitContentFile);

    Assertions.assertNotNull(datastream);
    Assertions.assertEquals(bagitContentFile.getDsid(), datastream.getDsid());
    Assertions.assertEquals(bagitContentFile.getMimetype(), datastream.getMimeType());
    Assertions.assertEquals(bagitContentFile.getSize(), datastream.getSize());
    Assertions.assertEquals(bagitContentFile.getTitle(), datastream.getBaseMetadata().getTitle());
    Assertions.assertEquals(bagitContentFile.getCreator(), datastream.getBaseMetadata().getCreator());
    Assertions.assertEquals(bagitContentFile.getDescription(), datastream.getBaseMetadata().getDescription());
    Assertions.assertEquals(bagitContentFile.getPublisher(), datastream.getBaseMetadata().getPublisher());
    Assertions.assertEquals(bagitContentFile.getRights(), datastream.getBaseMetadata().getRights());


  }





}
