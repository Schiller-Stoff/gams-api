package org.zim.gamsapi.Ingest.utils.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagitContentFile;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.TestUtilities.TestDatastream;

import java.util.Set;

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
    bagitContentFile.setTags(Set.of("tag1", "tag2"));
    bagitContentFile.setLang(Set.of("lang1", "lang2"));
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
    Assertions.assertEquals(bagitContentFile.getLang(), datastream.getLang());

  }

  @Test
  public void convertedBagContainsExpectedTags(){
    Datastream datastream = converter.convert(bagitContentFile);
    Assertions.assertEquals(bagitContentFile.getTags(), datastream.getTags());
  }

  @Test
  public void convertedBagContainsExpectedLang(){
    Datastream datastream = converter.convert(bagitContentFile);
    Assertions.assertEquals(bagitContentFile.getLang(), datastream.getLang());
  }

}
