package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.BatchIdXPathConfig;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGDEFIDR_XPATH;

@Slf4j
@Service
public class BatchIdXmlFieldExtractor {

    private final BatchIdXPathConfig batchIdXPathMap;

    @Autowired
    public BatchIdXmlFieldExtractor(BatchIdXPathConfig batchIdXPathMap) {
        this.batchIdXPathMap = batchIdXPathMap;
    }

    private static Document toXmlDocument(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
    }

    public String extractFieldByFileType(String xmlString) throws Exception {
        Document document = toXmlDocument(xmlString);
        String msgDefIdr = SfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);

        if (msgDefIdr == null) {
            log.info("MsgDefIdr not found in XML.");
        }

        String targetXPath = batchIdXPathMap.getXPathForFileType(msgDefIdr.trim());

        if (targetXPath == null) {
            // Log a warning or return null based on your use case
            log.info("Batch Id not present : {}", targetXPath);
            return null;
        }

        return SfmsConsmrCommonUtility.getValueByXPath(document, targetXPath);
    }
}

