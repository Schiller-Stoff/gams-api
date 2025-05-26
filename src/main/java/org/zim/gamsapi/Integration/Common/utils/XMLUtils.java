package org.zim.gamsapi.Integration.Common.utils;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zim.gamsapi.Integration.Common.exceptions.IntegrationDataProcessingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for handling XML - throwing useful HttpStatus
 * exception able to put through the Spring response error display.
 * (like for parsing byte arrays as documents etc.)
 */
@Slf4j
public class XMLUtils {

  /**
   * Parses given byte array to Document.
   * @param source source xml file
   * @return Parsed xml document
   * @throws IntegrationDataProcessingException parsing error
   */
  public static Document parseXml(byte[] source) throws IntegrationDataProcessingException {

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(
              new InputSource(new InputStreamReader(new ByteArrayInputStream(source), StandardCharsets.UTF_8))
      );
    } catch (ParserConfigurationException | SAXException | IOException e){
      String xml = new String(source, StandardCharsets.UTF_8);
      String msg = "Failed to parse given source datastream as XML." + e + "\n For XML: \n" + xml;
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
  }

  /**
   * Parses given InputStream to Document.
   * TODO test
   * @param source InputStream to be parsed
   * @return Parsed xml document
   * @throws IntegrationDataProcessingException
   */
  public static Document parseXml(InputStream source) throws IntegrationDataProcessingException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(
          new InputSource(new InputStreamReader(source, StandardCharsets.UTF_8))
      );
    } catch (ParserConfigurationException | SAXException | IOException e){
      String msg = "Failed to parse given source datastream as XML." + e + "\n";
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
  }


  /**
   * Transforms given Document to a byte array
   * @param document Document to be transformed
   * @return byte array representation of given document.
   * @throws IntegrationDataProcessingException when transformation to Document failed.
   */
  public static byte[] documentToByteArray(Document document) throws IntegrationDataProcessingException {
    DOMSource source = new DOMSource(document);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    StreamResult result= new StreamResult(bos);

    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "no");
      transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
      transformer.transform(source, result);
    } catch (TransformerException e){
      String msg = String.format("Failed to transform given XML Document to byte[] with cause: %s", e);
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
    return bos.toByteArray();
  }

  /**
   * Executes given xpath on given document. Xpath must return a NodeList
   * with at least one entry.
   * @param givenXpath xpath to execute on given document
   * @param document xml document to operate on
   * @return NodeList of found elements
   * @throws IntegrationDataProcessingException if xpath expression is invalid or no elements were found.
   */
  public static NodeList getAllXpath(String givenXpath, Document document) throws IntegrationDataProcessingException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    try {
      NodeList nodeList = (NodeList)xPath.compile(givenXpath).evaluate(document, XPathConstants.NODESET);
      if((nodeList == null) || (nodeList.getLength() == 0)){
        String msg = String.format("Found no (or at least one element!) %s  inside given document.",givenXpath);
        log.error(msg);
        throw new IntegrationDataProcessingException(msg);
      }
      return  nodeList;
    } catch (XPathExpressionException e){
      String msg = String.format("XPath on xml document failed. Got xpath: %s -  Original error: %s", givenXpath, e);
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

  }

  /**
   * Extracts text from given document.
   * @param document document to extract text from
   * @return String representation of extracted text. (trimmed, normalized) or empty string if no text was found.
   * @throws IntegrationDataProcessingException when extraction fails.
   */
  public static String extractText(Document document) throws IntegrationDataProcessingException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    String extractTextXpath = "//text()";

    try {
      NodeList nodeList = (NodeList) xPath.compile(extractTextXpath).evaluate(document, XPathConstants.NODESET);
      if(nodeList.getLength() == 0){
        log.warn("Found no text inside given document.");
        return "";
      }

      StringBuilder text = new StringBuilder();
      for (int i = 0; i < nodeList.getLength(); i++) {
        text.append(nodeList.item(i).getNodeValue());
      }
      //TODO refactor inefficient string manipulation
      String result = text.toString().trim().replaceAll("\n", " ");
      result = result.replaceAll("\t", " ");
      result = result.replaceAll("\\s+", " ");
      return result;
    } catch (XPathExpressionException e){
      String msg = String.format("XPath on xml document failed. Got xpath: %s -  Original error: %s", extractTextXpath, e);
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
  }


  /**
   * Extracts demanded attribute from given xml node.
   * @param attributeName name of attribute to extract
   * @param sourceNode node on which should be operated on
   * @return String value of requested attribute.
   * @throws IntegrationDataProcessingException when attribute extraction fails.
   */
  public static String extractAttributeValue(String attributeName, Node sourceNode) throws IntegrationDataProcessingException {
    // TODO how to build a propper error message?
    if((attributeName == null) || (attributeName.isEmpty())){
      String msg = "Given attributename is null or empty";
      //log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    String nodeName = sourceNode.getNodeName();
    if((nodeName == null) || (nodeName.isEmpty())){
        String msg = String.format("Cannot extract attribute %s from given node without tagname.", attributeName);
        //log.error(msg);
        throw new IntegrationDataProcessingException(msg);
    }

    NamedNodeMap attributes = sourceNode.getAttributes();
    if((attributes == null) || (attributes.getLength() == 0)){
      String msg = String.format("Failed to extract attribute %s from given node with name %s", attributeName, nodeName);
      //log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    Node attribute = attributes.getNamedItem(attributeName);
    if(attribute == null){
      String msg = String.format("Failed to extract attribute %s from node %s. Attribute is null (not available)", attributeName, nodeName);
      //log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    String attributeValue = attribute.getTextContent();
    if((attributeValue == null) || (attributeValue.isEmpty())){
      String msg = String.format("Failed to extract attribute %s from node %s. Attribute is defined but it's value is null or empty.", attributeValue, attributeName);
      //log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    return attributeValue;
  }


  /**
   * Adds specified attribute with given value on demanded xml-node.
   * @param attributeName Attribute to be set.
   * @param attributeValue value of the attribute.
   * @param sourceNode node to be processed.
   * @return refactored source node.
   * @throws IntegrationDataProcessingException if incoming values permit creation of attribute (like being null or empty)
   */
  public static Node applyAttributeValue(String attributeName, String attributeValue, Node sourceNode) throws IntegrationDataProcessingException {
    if((attributeName == null) || (attributeName.isEmpty())){
      String msg = "Failed to set attribute because given attribute name is null or empty";
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    if((attributeValue == null) || (attributeValue.isEmpty())){
      String msg = "Failed to set attribute because given attribute value is null or empty";
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    if(sourceNode == null){
      String msg = "Failed to set attribute because given xml-node is null";
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }

    ((Element)sourceNode).setAttribute(attributeName, attributeValue);

    return sourceNode;
  }

  /**
   * Extracts Dublin Core elements from given Dublin Core document.
   * @param dublinCore Dublin Core document to extract elements from.
   * @return List of extracted Dublin Core elements.
   */
  public static List<XMLElement> extractDCElements(Document dublinCore) {
    // TODO validate if dublin core?
    List<XMLElement> dcElements = new ArrayList<>();
    NodeList dublinCoreElements = XMLUtils.getAllXpath("/*/*", dublinCore);
    for(int i = 0; i < dublinCoreElements.getLength(); i++){
      String elementName = dublinCoreElements.item(i).getNodeName().replace("dc:", "");
      String elementValue = dublinCoreElements.item(i).getTextContent();
      XMLElement dcElement = XMLElement.builder()
          .name(elementName)
          .value(elementValue)
          .build();
      dcElements.add(dcElement);
    }
    return dcElements;
  }


  /**
   * Helper class for XML elements.
   */
  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class XMLElement {
    private String name;
    private String value;
  }


}
