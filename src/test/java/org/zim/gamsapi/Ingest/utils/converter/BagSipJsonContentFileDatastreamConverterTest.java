package org.zim.gamsapi.Ingest.utils.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Ingest.utils.Bagit.mapping.BagSipJsonContentFile;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.TestUtilities.TestDatastream;

import java.util.Set;

public class BagSipJsonContentFileDatastreamConverterTest extends UnitTest {


  BagSipJsonContentFile bagSipJsonContentFile;
  BagitSipJsonContentFileDatastreamConverter converter = new BagitSipJsonContentFileDatastreamConverter();

  @BeforeEach
  public void setUp(){
    bagSipJsonContentFile = new BagSipJsonContentFile();
    bagSipJsonContentFile.setDsid("dsid");
    bagSipJsonContentFile.setMimetype("mimetype");
    bagSipJsonContentFile.setSize(1L);
    bagSipJsonContentFile.setTitle("title");
    bagSipJsonContentFile.setCreator("creator");
    bagSipJsonContentFile.setDescription("description");
    bagSipJsonContentFile.setRights("rights");
    bagSipJsonContentFile.setTags(Set.of("tag1", "tag2"));
    bagSipJsonContentFile.setLang(Set.of("lang1", "lang2"));
  }


  @Test
  public void convertsExpectedBagitContentFileToDatastream() {

    bagSipJsonContentFile.setTags(TestDatastream.DATASTREAM_TAGS);
    Datastream datastream = converter.convert(bagSipJsonContentFile);
    Assertions.assertNotNull(datastream);
    Assertions.assertEquals(bagSipJsonContentFile.getDsid(), datastream.getDsid());
    Assertions.assertEquals(bagSipJsonContentFile.getMimetype(), datastream.getMimeType());
    Assertions.assertEquals(bagSipJsonContentFile.getSize(), datastream.getSize());
    Assertions.assertEquals(bagSipJsonContentFile.getTitle(), datastream.getBaseMetadata().getTitle());
    Assertions.assertEquals(bagSipJsonContentFile.getCreator(), datastream.getBaseMetadata().getCreator());
    Assertions.assertEquals(bagSipJsonContentFile.getDescription(), datastream.getBaseMetadata().getDescription());
    Assertions.assertEquals(bagSipJsonContentFile.getRights(), datastream.getBaseMetadata().getRights());
    Assertions.assertEquals(bagSipJsonContentFile.getTags(), datastream.getTags());
    Assertions.assertEquals(bagSipJsonContentFile.getLang(), datastream.getLang());

  }

  @Test
  public void convertedBagContainsExpectedTags(){
    Datastream datastream = converter.convert(bagSipJsonContentFile);
    Assertions.assertEquals(bagSipJsonContentFile.getTags(), datastream.getTags());
  }

  @Test
  public void convertedBagContainsExpectedLang(){
    Datastream datastream = converter.convert(bagSipJsonContentFile);
    Assertions.assertEquals(bagSipJsonContentFile.getLang(), datastream.getLang());
  }

}
