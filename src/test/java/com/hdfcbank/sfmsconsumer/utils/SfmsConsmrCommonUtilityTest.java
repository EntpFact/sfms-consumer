package com.hdfcbank.sfmsconsumer.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;

import static org.junit.jupiter.api.Assertions.*;

class SfmsConsmrCommonUtilityTest {

    private SfmsConsmrCommonUtility utility;

    @BeforeEach
    void setUp() {
        utility = new SfmsConsmrCommonUtility();
    }

    private Document loadXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // important for ISO20022 style XML
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new java.io.ByteArrayInputStream(xml.getBytes()));
    }

    @Test
    void testGetValueByXPath_validElement() throws Exception {
        String xml = "<RequestPayload><AppHdr><BizMsgIdr>RBIP12345</BizMsgIdr></AppHdr></RequestPayload>";
        Document doc = loadXml(xml);

        String result = utility.getValueByXPath(doc, "//*[local-name()='BizMsgIdr']");

        assertEquals("RBIP12345", result);
    }

    @Test
    void testGetValueByXPath_missingElement_returnsNull() throws Exception {
        String xml = "<RequestPayload><AppHdr></AppHdr></RequestPayload>";
        Document doc = loadXml(xml);

        String result = utility.getValueByXPath(doc, "//*[local-name()='BizMsgIdr']");

        assertNull(result);
    }

    @Test
    void testGetValueByXPath_elementWithWhitespace_trimmed() throws Exception {
        String xml = "<RequestPayload><AppHdr><BizMsgIdr>  RBIP67890  </BizMsgIdr></AppHdr></RequestPayload>";
        Document doc = loadXml(xml);

        String result = utility.getValueByXPath(doc, "//*[local-name()='BizMsgIdr']");

        assertEquals("RBIP67890", result);
    }

    @Test
    void testGetValueByXPath_invalidXPath_throwsException() throws Exception {
        String xml = "<RequestPayload><AppHdr><BizMsgIdr>RBIP11111</BizMsgIdr></AppHdr></RequestPayload>";
        Document doc = loadXml(xml);

        assertThrows(XPathExpressionException.class, () ->
                utility.getValueByXPath(doc, "//*invalid_xpath###")
        );
    }
}
