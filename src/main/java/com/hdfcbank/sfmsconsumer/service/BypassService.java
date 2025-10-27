package com.hdfcbank.sfmsconsumer.service;


import com.hdfcbank.sfmsconsumer.config.BypassProperties;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BypassService {

    private static final Logger log = LoggerFactory.getLogger(BypassService.class);

    private KafkaUtils kafkaUtils;
    private final BypassProperties bypassProperties;



    public boolean isBypassEnabled() {
        return bypassProperties.isEnabled();
    }

    public void sendToBypassSwitch(String payload) {
        String switchName = bypassProperties.getDefaultSwitch();
        if (switchName == null || switchName.isBlank()) {
            throw new IllegalStateException("No default switch configured in application.yml");
        }

        String topic = bypassProperties.getTopicForSwitch(switchName);
        if (topic == null) {
            throw new IllegalArgumentException("No topic configured for switch: " + switchName);
        }

        log.info("Bypass enabled — sending message to switch '{}' (topic '{}')", switchName, topic);
        kafkaUtils.publishToResponseTopic(topic, payload);
    }
}
