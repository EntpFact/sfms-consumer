package com.hdfcbank.sfmsconsumer.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@Slf4j
@Component
public class SfmsConsmrCommonUtility {

    public String getValueByXPath(Document document, String xpathExpression) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node resultNode = (Node) xpath.evaluate(xpathExpression, document, XPathConstants.NODE);
        return resultNode != null ? resultNode.getTextContent().trim() : null;
    }

}
