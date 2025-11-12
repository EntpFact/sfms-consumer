package com.hdfcbank.sfmsconsumer.kafkaproducer;


import com.hdfcbank.messageconnect.config.PubSubOptions;
import com.hdfcbank.messageconnect.dapr.producer.DaprProducer;
import com.hdfcbank.sfmsconsumer.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class KafkaUtilsTest {

    @Mock
    private DaprProducer daprProducer;

    @InjectMocks
    private KafkaUtils kafkaUtils;

    private final String message = "{\"sample\":\"test\"}";
    private final String topic = "test-topic";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
/*
    @Test
    void testPublishToResponseTopic_success() {
        // Mock daprProducer to return a successful Mono
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("success"));

        kafkaUtils.publishToResponseTopic(message, topic);

        // Verify daprProducer was called once
        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    void testPublishToResponseTopic_error() {
        // Mock daprProducer to return an error Mono
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.error(new RuntimeException("Kafka publish failed")));

        kafkaUtils.publishToResponseTopic(message, topic);

        // Verify daprProducer was still called
        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    void testPublishToResponseTopic_metadataValidation() {
        // Capture PubSubOptions to validate metadata and topic
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenAnswer(invocation -> {
                    PubSubOptions pubSubOptions = invocation.getArgument(0);
                    // Assertions on PubSubOptions
                    assert pubSubOptions.getTopic().equals(topic);
                    assert pubSubOptions.getPubsubName().equals(Constants.KAFKA_RESPONSE_TOPIC_DAPR_BINDING);
                    Map<String, String> metadata = pubSubOptions.getMetadata();
                    assert metadata.containsKey("rawPayload");
                    assert metadata.get("rawPayload").equals("true");
                    return Mono.just("success");
                });

        kafkaUtils.publishToResponseTopic(message, topic);

        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }*/
}
