package com.hdfcbank.sfmsconsumer.service;


import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;

@Slf4j
@Service
@AllArgsConstructor
public class IncomingMsgAudit {

    @Autowired
    SFMSConsumerRepository SFMSConsumerRepository;

    @Autowired
    SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    private final TargetProcessorTopicConfig config;

    @Autowired
    BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @Autowired
    private ErrXmlRoutingService errorMsgAudit;

    @Autowired
    BuildJsonReq buildJsonReq;

    @Transactional
    public void auditIncomingMessage(String xmlMessage[]) {

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlMessage[1])));
            String msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
            String msgType = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
            String target = config.getProcessorFileType(msgType);

            //Get batchID
            String batchId = batchIdXmlFieldExtractor.extractFieldByFileType(xmlMessage[1]);

            // Get json request.
            String jsonReq = buildJsonReq.buildRequest(xmlMessage);

            if (msgType.contains(ADMI)) {
                AdmiTracker admiTracker = AdmiTracker.builder()
                        .msgId(msgId)
                        .msgType(msgType)
                        .target(target)
                        .invalidReq(false)
                        .transformedJsonReq(jsonReq)
                        .orgnlReq(xmlMessage[0] + xmlMessage[1])
                        .build();

                SFMSConsumerRepository.saveDataInAdmiTracker(admiTracker);


            } else {

                MsgEventTracker msgEventTracker = MsgEventTracker.builder()
                        .msgId(msgId)
                        .msgType(msgType)
                        .source(SFMS)
                        .flowType(INWARD)
                        .invalidReq(false)
                        .transformedJsonReq(jsonReq)
                        .target(target)
                        .batchId(batchId != null ? batchId : "")
                        .orgnlReq(xmlMessage[0] + xmlMessage[1])
                        .build();

                SFMSConsumerRepository.saveDataInMsgEventTracker(msgEventTracker);
            }
/*
            BatchTracker batchTracker = BatchTracker.builder()
                    .batchId(batchId!=null ? batchId: "")
                    .msgId(msgId)
                    .msgType(msgType)
                    .status(CAPTURED)
                    .build();

            SFMSConsumerRepository.saveMsgInBatchTracker(batchTracker);*/

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            log.error("Error Message received : {}", e);
            errorMsgAudit.determineTopic(xmlMessage[0]+xmlMessage[1]);
        } catch (Exception e) {
            errorMsgAudit.determineTopic(xmlMessage[0]+xmlMessage[1]);
            log.error("Error Message received : {}", e);
            throw new RuntimeException(e);
        }

    }

}
