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

    public void sendRequest(String xml) {

        try {

            String key = XmlFieldExtractor.extractFieldByFileType(xml);
            System.out.println(key);

            // Send to nil-router topic
            kafkaUtils.publishToResponseTopic(xml, nilrouter, key);
            // Send to message-event-tracker-service topic
            kafkaUtils.publishToResponseTopic(xml, msgEventTracker, key);


        } catch (Exception e) {
            log.error(e.toString());
        }
    }

}