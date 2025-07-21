package com.hdfcbank.sfmsconsumer.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;

@Slf4j
@Component
public class SfmsConsmrCommonUtility {

    public static String getValueByXPath(Document document, String xpathExpression) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node resultNode = (Node) xpath.evaluate(xpathExpression, document, XPathConstants.NODE);
        return resultNode != null ? resultNode.getTextContent().trim() : null;
    }

}
