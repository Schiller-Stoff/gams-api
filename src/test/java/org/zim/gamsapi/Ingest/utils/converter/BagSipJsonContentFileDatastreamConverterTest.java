package org.zim.gamsapi.Ingest.utils.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagitSipJsonContentFile;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.TestUtilities.TestDatastream;

import java.util.Set;

public class BagSipJsonContentFileDatastreamConverterTest extends UnitTest {


  BagitSipJsonContentFile bagitSipJsonContentFile;
  BagitSipJsonContentFileDatastreamConverter converter = new BagitSipJsonContentFileDatastreamConverter();

  @BeforeEach
  public void setUp(){
    bagitSipJsonContentFile = new BagitSipJsonContentFile();
    bagitSipJsonContentFile.setDsid("dsid");
    bagitSipJsonContentFile.setMimetype("mimetype");
    bagitSipJsonContentFile.setSize(1L);
    bagitSipJsonContentFile.setTitle("title");
    bagitSipJsonContentFile.setCreator("creator");
    bagitSipJsonContentFile.setDescription("description");
    bagitSipJsonContentFile.setRights("rights");
    bagitSipJsonContentFile.setTags(Set.of("tag1", "tag2"));
    bagitSipJsonContentFile.setLang(Set.of("lang1", "lang2"));
  }


  @Test
  public void convertsExpectedBagitContentFileToDatastream() {

    bagitSipJsonContentFile.setTags(TestDatastream.DATASTREAM_TAGS);
    Datastream datastream = converter.convert(bagitSipJsonContentFile);
    Assertions.assertNotNull(datastream);
    Assertions.assertEquals(bagitSipJsonContentFile.getDsid(), datastream.getDsid());
    Assertions.assertEquals(bagitSipJsonContentFile.getMimetype(), datastream.getMimeType());
    Assertions.assertEquals(bagitSipJsonContentFile.getSize(), datastream.getSize());
    Assertions.assertEquals(bagitSipJsonContentFile.getTitle(), datastream.getBaseMetadata().getTitle());
    Assertions.assertEquals(bagitSipJsonContentFile.getCreator(), datastream.getBaseMetadata().getCreator());
    Assertions.assertEquals(bagitSipJsonContentFile.getDescription(), datastream.getBaseMetadata().getDescription());
    Assertions.assertEquals(bagitSipJsonContentFile.getRights(), datastream.getBaseMetadata().getRights());
    Assertions.assertEquals(bagitSipJsonContentFile.getTags(), datastream.getTags());
    Assertions.assertEquals(bagitSipJsonContentFile.getLang(), datastream.getLang());

  }

  @Test
  public void convertedBagContainsExpectedTags(){
    Datastream datastream = converter.convert(bagitSipJsonContentFile);
    Assertions.assertEquals(bagitSipJsonContentFile.getTags(), datastream.getTags());
  }

  @Test
  public void convertedBagContainsExpectedLang(){
    Datastream datastream = converter.convert(bagitSipJsonContentFile);
    Assertions.assertEquals(bagitSipJsonContentFile.getLang(), datastream.getLang());
  }

}
