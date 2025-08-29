package com.hdfcbank.sfmsconsumer.service;


import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.NilRepository;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.BatchTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
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

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;

@Slf4j
@Service
@AllArgsConstructor
public class IncomingMsgAudit {

    @Autowired
    NilRepository nilRepository;

    @Autowired
    SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    private final TargetProcessorTopicConfig config;

    @Autowired
    BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

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


            if (msgType.contains(ADMI)) {
                AdmiTracker admiTracker = AdmiTracker.builder()
                        .msgId(msgId)
                        .msgType(msgType)
                        .target(target)
                        .invalidReq(false)
                        .orgnlReq(xmlMessage[0] + xmlMessage[1])
                        .build();

                nilRepository.saveDataInAdmiTracker(admiTracker);


            } else {

                MsgEventTracker msgEventTracker = new MsgEventTracker();
                msgEventTracker.setMsgId(msgId);
                msgEventTracker.setMsgType(msgType);
                msgEventTracker.setSource(SFMS);
                msgEventTracker.setInvalidReq(false);
                msgEventTracker.setTarget(target);
                msgEventTracker.setBatchId(batchId!=null ? batchId: "");
                msgEventTracker.setOrgnlReq(xmlMessage[0] + xmlMessage[1]);
                nilRepository.saveDataInMsgEventTracker(msgEventTracker);
            }

            BatchTracker batchTracker = BatchTracker.builder()
                    .batchId(batchId!=null ? batchId: "")
                    .msgId(msgId)
                    .msgType(msgType)
                    .status(CAPTURED)
                    .build();

            nilRepository.saveMsgInBatchTracker(batchTracker);

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            log.error("Error Message received : {}", e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
