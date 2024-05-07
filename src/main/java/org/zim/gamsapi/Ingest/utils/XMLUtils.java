package org.zim.gamsapi.Ingest.utils;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zim.gamsapi.Ingest.exceptions.IngestProcessingException;

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
   * @throws IngestProcessingException parsing error
   */
  public static Document parseXml(byte[] source) throws IngestProcessingException {

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
      throw new IngestProcessingException(msg);
    }
  }

  /**
   * Transforms given Document to a byte array
   * @param document Document to be transformed
   * @return byte array representation of given document.
   * @throws IngestProcessingException when transformation to Document failed.
   */
  public static byte[] documentToByteArray(Document document) throws IngestProcessingException {
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
      throw new IngestProcessingException(msg);
    }
    return bos.toByteArray();
  }

  /**
   * Executes given xpath on given document. Xpath must return a NodeList
   * with at least one entry.
   * @param givenXpath xpath to execute on given document
   * @param document xml document to operate on
   * @return NodeList of found elements
   * @throws IngestProcessingException if xpath expression is invalid or no elements were found.
   */
  public static NodeList getAllXpath(String givenXpath, Document document) throws IngestProcessingException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    try {
      NodeList nodeList = (NodeList)xPath.compile(givenXpath).evaluate(document, XPathConstants.NODESET);
      if((nodeList == null) || (nodeList.getLength() == 0)){
        String msg = String.format("Found no (or at least one element!) %s  inside given document.",givenXpath);
        log.error(msg);
        throw new IngestProcessingException(msg);
      }
      return  nodeList;
    } catch (XPathExpressionException e){
      String msg = String.format("XPath on xml document failed. Got xpath: %s -  Original error: %s", givenXpath, e);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

  }

  /**
   * Extracts demanded attribute from given xml node.
   * @param attributeName name of attribute to extract
   * @param sourceNode node on which should be operated on
   * @return String value of requested attribute.
   * @throws IngestProcessingException when attribute extraction fails.
   */
  public static String extractAttributeValue(String attributeName, Node sourceNode) throws IngestProcessingException {
    // TODO how to build a propper error message?
    if((attributeName == null) || (attributeName.isEmpty())){
      String msg = "Given attributename is null or empty";
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    String nodeName = sourceNode.getNodeName();
    if((nodeName == null) || (nodeName.isEmpty())){
        String msg = String.format("Cannot extract attribute %s from given node without tagname.", attributeName);
        log.error(msg);
        throw new IngestProcessingException(msg);
    }

    NamedNodeMap attributes = sourceNode.getAttributes();
    if((attributes == null) || (attributes.getLength() == 0)){
      String msg = String.format("Failed to extract attribute %s from given node with name %s", attributeName, nodeName);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    Node attribute = attributes.getNamedItem(attributeName);
    if(attribute == null){
      String msg = String.format("Failed to extract attribute %s from node %s. Attribute is null (not available)", attributeName, nodeName);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    String attributeValue = attribute.getTextContent();
    if((attributeValue == null) || (attributeValue.isEmpty())){
      String msg = String.format("Failed to extract attribute %s from node %s. Attribute is defined but it's value is null or empty.", attributeValue, attributeName);
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    return attributeValue;
  }


  /**
   * Adds specified attribute with given value on demanded xml-node.
   * @param attributeName Attribute to be set.
   * @param attributeValue value of the attribute.
   * @param sourceNode node to be processed.
   * @return refactored source node.
   * @throws IngestProcessingException if incoming values permit creation of attribute (like being null or empty)
   */
  public static Node applyAttributeValue(String attributeName, String attributeValue, Node sourceNode) throws IngestProcessingException {
    if((attributeName == null) || (attributeName.isEmpty())){
      String msg = "Failed to set attribute because given attribute name is null or empty";
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    if((attributeValue == null) || (attributeValue.isEmpty())){
      String msg = "Failed to set attribute because given attribute value is null or empty";
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    if(sourceNode == null){
      String msg = "Failed to set attribute because given xml-node is null";
      log.error(msg);
      throw new IngestProcessingException(msg);
    }

    ((Element)sourceNode).setAttribute(attributeName, attributeValue);

    return sourceNode;
  }

}
