package com.hdfcbank.sfmsconsumer.kafkaproducer;

import com.hdfcbank.messageconnect.config.PubSubOptions;
import com.hdfcbank.messageconnect.dapr.producer.DaprProducer;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.exception.SFMSConsumerException;
import com.hdfcbank.sfmsconsumer.utils.Constants;
import io.dapr.client.domain.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class KafkaUtils {

    @Autowired
    DaprProducer daprProducer;

    @Autowired
    private SFMSConsumerRepository sfmsConsumerRepository;


    public Mono<Void> publishToKafkaTopic(String message, String topic, String msgid) {

        /*Map<String, String> metadata = new HashMap<>();
        metadata.put("partitionKey", msgid);

        var kafkaBinding = PubSubOptions.builder()
                .requestData(message)
                .topic(topic)
                .pubsubName(Constants.KAFKA_RESPONSE_TOPIC_DAPR_BINDING)
                .metadata(metadata)
                .build();
        return Mono.fromRunnable(() ->
                        log.info("Mock publish success for msgId={} topic={}", msgid, topic)
                )
                .then();*/
        Map<String, String> metadata = new HashMap<>();
        metadata.put("partitionKey", msgid);

        var cloudEvents = buildCloudEvent(topic, message, msgid);
        var resp = daprProducer.invokeDaprPublishEvent(cloudEvents, metadata);

        resp.doOnSuccess(res -> log.info("Message published successfully to topic: {}", topic))
                .onErrorMap(e -> {
                    log.error("Error while publishing message to Kafka topic: {}", topic, e);
                    throw new SFMSConsumerException.KafkaException(e.getMessage(),message, e);
                })
                .share().block(); // executes synchronously
        return null;
    }

    public CloudEvent buildCloudEvent(String kafkaTopic, String events, String msgId) {
        var cloudEvent = new CloudEvent();
        cloudEvent.setId(msgId);
        cloudEvent.setTraceParent(msgId);
        //   cloudEvent.setTraceId(msgId); //Deprecated
        cloudEvent.setTopic(kafkaTopic);
        cloudEvent.setPubsubName(Constants.DAPR_PUBSUB_COMPONENT);
        cloudEvent.setDatacontenttype(Constants.CLOUD_EVENT_CONTENT_TYPE);
        cloudEvent.setSource(Constants.SERVICE_NAME);
        cloudEvent.setTime(OffsetDateTime.now());
        cloudEvent.setType(Constants.CLOUD_EVENT_TYPE);
        cloudEvent.setData(events);
        cloudEvent.setSpecversion(Constants.CLOUD_EVENT_SPEC_VERSION);
        return cloudEvent;
    }

   /* public Mono<Void> publishToResponseTopic(String message, String topic, String msgId, String batchId) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("partitionKey", msgId);

        var kafkaBinding = PubSubOptions.builder()
                .requestData(message)
                .topic(topic)
                .pubsubName(Constants.KAFKA_RESPONSE_TOPIC_DAPR_BINDING)
                .metadata(metadata)
                .build();
        return Mono.fromRunnable(() ->
                        log.info("Mock publish success for msgId={} topic={}", msgId, topic)
                )
                .then();
*//*        return daprProducer.invokeDaprPublishEvent(kafkaBinding)
                .doOnSuccess(res -> log.info("Published message {} to topic {}", msgId, topic))
                .then() // ensure success path is Mono<Void>
                .onErrorResume(ex -> {
                    log.error(" Failed to publish message {}: {}", msgId, ex.getMessage(), ex);

                    // Update TECX and propagate an error so offset isn't committed
                    return sfmsConsumerRepository.updateStatusToTecx(msgId)
                            .then(Mono.error(new RuntimeException("Kafka publish failed", ex)));
                });*//*
    }*/
}
