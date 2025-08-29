package com.hdfcbank.sfmsconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.Body;
import com.hdfcbank.sfmsconsumer.model.Header;
import com.hdfcbank.sfmsconsumer.model.ReqPayload;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;


@Slf4j
@Service
@AllArgsConstructor
public class PublishMessage {
    private final TargetProcessorTopicConfig config;

    @Autowired
    SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Autowired
    KafkaUtils kafkaUtils;

    @Autowired
    ErrXmlRoutingService errXmlRoutingService;

    public void sendRequest(String xml[]) {
        String xmlMessage = xml[0] + xml[1];
        try {

            Document document = toXmlDocument(xml[1]);
            String msgDefIdr = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
            String msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
            String target = config.getProcessorFileType(msgDefIdr.trim());

            log.info("msgDefIdr : {}", msgDefIdr);
            log.info("Xml Message : " + xmlMessage);


            Header header = Header.builder()
                    .msgId(msgId)
                    .msgType(msgDefIdr)
                    .source(SFMS)
                    .target(target)
                    .flowType(INWARD)
                    .replayInd(false)
                    .prefix(xml[0])
                    .build();

            Body body = Body.builder()
                    .payload(xml[1])
                    .build();

            ReqPayload reqPayload = ReqPayload.builder()
                    .header(header)
                    .body(body)
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(reqPayload);


            String topic = config.getTopicFileType(msgDefIdr.trim());

            // Send to respective processor topic
            log.info("JSON : {}", json);
            kafkaUtils.publishToResponseTopic(json, topic);


        } catch (Exception e) {
            log.error(e.toString());
            errXmlRoutingService.determineTopic(xmlMessage);
        }
    }

    private static Document toXmlDocument(String xmlString) {
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

}