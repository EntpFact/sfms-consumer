package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class XmlFieldExtractor {

    // Define field-specific XPaths per file type
    private static final Map<String, String> fileTypeToXPathMap = new HashMap<>();

    static {
        fileTypeToXPathMap.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        fileTypeToXPathMap.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        fileTypeToXPathMap.put("pacs.002.001.11", "(.//*[local-name()='StsRsnInf']//*[local-name()='AddtlInf'])[1]");
        fileTypeToXPathMap.put("camt.052.001.08", ".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']");
        fileTypeToXPathMap.put("camt.054.001.08", ".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']");
        // Add more file types and corresponding XPaths as needed
    }

    public static String extractFieldByFileType(String xmlString) throws Exception {
        Document document = toXmlDocument(xmlString);
        String msgDefIdr = SfmsConsmrCommonUtility.getValueByXPath(document, "//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']");

        if (msgDefIdr == null) {
            throw new IllegalArgumentException("MsgDefIdr not found in XML.");
        }

        String targetXPath = fileTypeToXPathMap.get(msgDefIdr.trim());

        if (targetXPath == null) {
            return null;
        }

        return SfmsConsmrCommonUtility.getValueByXPath(document, targetXPath);
    }

    private static Document toXmlDocument(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
    }
}

