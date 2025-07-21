package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.dao.NilRepository;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;

@Slf4j
@Service
public class DedupCheck {

    @Autowired
    private NilRepository nilRepository;

    public MsgEventTracker checkDuplicate(String xml) {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            String msgId = SfmsConsmrCommonUtility.getValueByXPath(document, "//*[local-name()='AppHdr']/*[local-name()='BizMsgIdr']");

            //check duplicate
            MsgEventTracker duplicateEntry = nilRepository.findByMsgId(msgId);
            if (duplicateEntry != null) {
                nilRepository.saveDuplicateEntry(duplicateEntry);
                return duplicateEntry; // Skip processing if duplicate found
            }


        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
