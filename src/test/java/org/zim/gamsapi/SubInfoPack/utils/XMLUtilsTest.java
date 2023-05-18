package org.zim.gamsapi.SubInfoPack.utils;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zim.gamsapi.SubInfoPack.exceptions.SubInfoPackProcessingException;
import org.zim.gamsapi.UnitTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class XMLUtilsTest extends UnitTest {

  private final String TESTFOLDER_LOCATION = "testfiles/tei";
  private final String TESTFILE_LOCATION = TESTFOLDER_LOCATION + "/TEI_SOURCE.xml";

  @Nested
  public class ParseXml {

    @Test
    public void parsedXmlContainsExpectedRootTag() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      Assertions.assertThat(teiDocument.getDocumentElement().getTagName()).isEqualTo("TEI");
    }

    @Test
    public void parsedXmlContainsExpectedIdno() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      String idnoValue = teiDocument.getElementsByTagName("idno").item(0).getTextContent();
      Assertions.assertThat(idnoValue).isEqualTo("o:test.tei");
    }

  }

  @Nested
  public class DocumentToByteArray {


    /**
     * Tests if source byte array and transformed byte array have somewhat the same length.
     * There might be differences because of additionally applied processing instructions added to the transformed
     * XML document.
     * @throws IOException if test file was not found
     */
    @Test
    public void hasNearlyTheSameByteCount() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      byte[] testData = teiIngestXML.readAllBytes();
      Document teiDocument = XMLUtils.parseXml(testData);
      byte[] transformedDocument =  XMLUtils.documentToByteArray(teiDocument);
      Assertions.assertThat(transformedDocument.length).isCloseTo(testData.length, Percentage.withPercentage(2));
    }


    @Test
    public void hasExpectedAngleBracketsCounts() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      byte[] testData = teiIngestXML.readAllBytes();
      String testIngestXmlAsString = new String(testData, StandardCharsets.UTF_8);
      Document teiDocument = XMLUtils.parseXml(testData);

      byte[] transformedDocument =  XMLUtils.documentToByteArray(teiDocument);
      String transformedDocumentAsString = new String(transformedDocument, StandardCharsets.UTF_8);

      long originalLTOccurence = testIngestXmlAsString.chars().filter(ch -> ch == '<').count();
      long resultLTOccurence = transformedDocumentAsString.chars().filter(ch -> ch == '<').count();
      Assertions.assertThat(originalLTOccurence).isEqualTo(resultLTOccurence);

      long originalGTOccurence = testIngestXmlAsString.chars().filter(ch -> ch == '>').count();
      long resultGTOccurence = transformedDocumentAsString.chars().filter(ch -> ch == '>').count();
      Assertions.assertThat(originalGTOccurence).isEqualTo(resultGTOccurence);

    }

    @Test
    public void transformationIsNotEmpty() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      byte[] testData = teiIngestXML.readAllBytes();
      Document teiDocument = XMLUtils.parseXml(testData);
      byte[] transformedDocument =  XMLUtils.documentToByteArray(teiDocument);
      Assertions.assertThat(transformedDocument.length).isGreaterThan(0);
    }

  }

  @Nested
  public class GetAllXpath {

    @Test
    public void returnsExpectedNodeCount() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      NodeList nodeList = XMLUtils.getAllXpath("//title", teiDocument);
      Assertions.assertThat(nodeList.getLength()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fail", "2313", "/head", "", "//NOT_FOUND"})
    public void throwsIfMAlformedXpathWasGiven(String xpath) throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      org.junit.jupiter.api.Assertions.assertThrows(SubInfoPackProcessingException.class, () -> {
        XMLUtils.getAllXpath(xpath, teiDocument);
      });
    }

  }


  @Nested
  public class ExtractAttributeValue {


    @ParameterizedTest
    @ValueSource(strings = {"", "NOT_FOUND", "_"})
    public void failsIfIncorretInput(String attributeName) throws IOException {

      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      NodeList idnoList = teiDocument.getElementsByTagName("idno");
      Node pidIdno = idnoList.item(0);

      org.junit.jupiter.api.Assertions.assertThrows(SubInfoPackProcessingException.class, () -> {
        XMLUtils.extractAttributeValue(attributeName, pidIdno);
      });

    }

    @Test
    public void extractsExpectedAttributeValue() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      NodeList idnoList = teiDocument.getElementsByTagName("idno");
      Node pidIdno = idnoList.item(0);
      String typeAttributeValue = XMLUtils.extractAttributeValue("type", pidIdno);
      Assertions.assertThat(typeAttributeValue).isEqualTo("PID");
    }

  }

  @Nested
  public class ApplyAttributeValue {

    @Test
    public void appliesExpectedAttribute() throws IOException {
      InputStream teiIngestXML = new ClassPathResource(TESTFILE_LOCATION).getInputStream();
      Document teiDocument = XMLUtils.parseXml(teiIngestXML.readAllBytes());
      NodeList idnoList = teiDocument.getElementsByTagName("idno");
      Node pidIdno = idnoList.item(0);
      XMLUtils.applyAttributeValue("rudi", "kanone", pidIdno);
      String actualAttributeValue = XMLUtils.extractAttributeValue("rudi", pidIdno);
      Assertions.assertThat(actualAttributeValue).isEqualTo("kanone");

    }

  }

}
