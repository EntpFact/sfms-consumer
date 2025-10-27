package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.BatchIdXPathConfig;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGDEFIDR_XPATH;
import static com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility.toXmlDocument;

@Slf4j
@Service
@AllArgsConstructor
public class BatchIdXmlFieldExtractor {

    @Autowired
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    private final BatchIdXPathConfig batchIdXPathMap;


    public String extractFieldByFileType(String xmlString) throws Exception {
        Document document = toXmlDocument(xmlString);
        String msgDefIdr = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);

        if (msgDefIdr == null) {
            log.info("MsgDefIdr not found in XML.");
            return null;
        }

        String targetXPath = batchIdXPathMap.getXPathForFileType(msgDefIdr.trim());

        if (targetXPath == null || targetXPath.trim().isEmpty()) {
            // Log a warning or return null based on your use case
            log.info("Batch Id not present : {}", targetXPath);
            return "";
        }

        return sfmsConsmrCommonUtility.getValueByXPath(document, targetXPath);
    }
}

