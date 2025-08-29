package com.hdfcbank.sfmsconsumer.kafkaproducer;

import com.hdfcbank.messageconnect.config.PubSubOptions;
import com.hdfcbank.messageconnect.dapr.producer.DaprProducer;
import com.hdfcbank.sfmsconsumer.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.hdfcbank.sfmsconsumer.utils.Constants.RAW_PAYLOAD;
import static com.hdfcbank.sfmsconsumer.utils.Constants.TRUE;

@Slf4j
@Service
public class KafkaUtils {

    @Autowired
    DaprProducer daprProducer;


    public void publishToResponseTopic(String message, String topic) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put(RAW_PAYLOAD, TRUE);  // optional, for raw XML/string

        var kafkaBinding = PubSubOptions.builder().requestData(message).topic(topic)
                .pubsubName(Constants.KAFKA_RESPONSE_TOPIC_DAPR_BINDING)
                .metadata(metadata)
                .build();
        var resp = daprProducer.invokeDaprPublishEvent(kafkaBinding);
        resp.doOnSuccess(res -> {
            log.info("Response published to response topic successfully");
        }).onErrorResume(res -> {
            log.info("Error on publishing the response to response topic");
            return Mono.empty();
        }).share().block();

    }
}
