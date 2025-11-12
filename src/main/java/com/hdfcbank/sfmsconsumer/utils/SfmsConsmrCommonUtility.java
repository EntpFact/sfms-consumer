package com.hdfcbank.sfmsconsumer.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SfmsConsmrCommonUtility {

    public String getValueByXPath(Document document, String xpathExpression) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String value = (String) xpath.evaluate(xpathExpression, document, XPathConstants.STRING);
        return value != null && !value.isEmpty() ? value.trim() : null;
    }


    public static Document toXmlDocument(String xmlString) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();

            return builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMsgId(String xmlMessage) {
        Pattern pattern = Pattern.compile("<\\s*BizMsgIdr\\s*>(.*?)<\\s*/\\s*BizMsgIdr\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xmlMessage);
        if (matcher.find()) return matcher.group(1);
        log.error("MsgId not found in XML");
        return null;
    }

    public String extractBatchIdValue(String xml) {
        Pattern pattern = Pattern.compile("<(Ustrd|AddtlInf)>(.*?)</\\1>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) return matcher.group(2);
        return "";
    }



    public String getMsgType(String xmlMessage) {
        Pattern pattern = Pattern.compile("<\\s*MsgDefIdr\\s*>(.*?)<\\s*/\\s*MsgDefIdr\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xmlMessage);
        if (matcher.find()) return matcher.group(1);
        log.error("MsgDefIdr not found in XML");
        return null;
    }

}
