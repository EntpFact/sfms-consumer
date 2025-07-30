package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class PublishMessage {

    @Value("${topic.nilrouter}")
    private String nilrouter;

    @Value("${topic.msgeventtracker}")
    private String msgEventTracker;

    @Autowired
    KafkaUtils kafkaUtils;

    @Autowired
    BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    public void sendRequest(String xml[]) {
        String xmlMessage = xml[0] + xml[1];
        try {

            String key = batchIdXmlFieldExtractor.extractFieldByFileType(xml[1]);
            log.info("Key : {}", key);
            log.info("Xml Message : " + xmlMessage);

            // Send to nil-router topic
            kafkaUtils.publishToResponseTopic(xmlMessage, nilrouter, key);
            // Send to message-event-tracker-service topic
            kafkaUtils.publishToResponseTopic(xmlMessage, msgEventTracker, key);


        } catch (Exception e) {
            log.error(e.toString());

            // Send to error message to nil-router / message-event-tracker-service topic
            kafkaUtils.publishToResponseTopic(xmlMessage, nilrouter, "error");
            kafkaUtils.publishToResponseTopic(xmlMessage, msgEventTracker, "error");
        }
    }

}