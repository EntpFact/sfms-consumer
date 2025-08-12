package com.hdfcbank.sfmsconsumer.kafkaproducer;

import com.hdfcbank.messageconnect.config.PubSubOptions;
import com.hdfcbank.messageconnect.dapr.producer.DaprProducer;
import com.hdfcbank.sfmsconsumer.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaUtils Tests")
class KafkaUtilsTest {

    @Mock
    private DaprProducer daprProducer;

    @InjectMocks
    private KafkaUtils kafkaUtils;

    private String testMessage;
    private String testTopic;
    private String testKey;

    @BeforeEach
    void setUp() {
        testMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><element>test</element></root>";
        testTopic = "test-topic";
        testKey = "test-key";
    }

    @Test
    @DisplayName("Should publish message to response topic successfully")
    void shouldPublishMessageToResponseTopicSuccessfully() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    @DisplayName("Should publish message with correct PubSubOptions configuration")
    void shouldPublishMessageWithCorrectPubSubOptionsConfiguration() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(testMessage, pubSubOptions.getRequestData());
            assertEquals(testTopic, pubSubOptions.getTopic());
            assertEquals(Constants.KAFKA_RESPONSE_TOPIC_DAPR_BINDING, pubSubOptions.getPubsubName());
            
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertNotNull(metadata);
            assertEquals("true", metadata.get("rawPayload"));
            assertEquals(testKey, metadata.get("key"));
            
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        // Given
        String emptyMessage = "";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(emptyMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(emptyMessage, pubSubOptions.getRequestData());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(null, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertNull(pubSubOptions.getRequestData());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle empty topic")
    void shouldHandleEmptyTopic() {
        // Given
        String emptyTopic = "";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, emptyTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(emptyTopic, pubSubOptions.getTopic());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle null topic")
    void shouldHandleNullTopic() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, null, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertNull(pubSubOptions.getTopic());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle empty key")
    void shouldHandleEmptyKey() {
        // Given
        String emptyKey = "";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, emptyKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertEquals(emptyKey, metadata.get("key"));
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle null key")
    void shouldHandleNullKey() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, null);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertNull(metadata.get("key"));
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void shouldHandleSpecialCharactersInMessage() {
        // Given
        String messageWithSpecialChars = "<?xml version=\"1.0\"?><root><element>!@#$%^&*()_+-=[]{}|;':\",./<>?</element></root>";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(messageWithSpecialChars, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(messageWithSpecialChars, pubSubOptions.getRequestData());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle unicode characters in message")
    void shouldHandleUnicodeCharactersInMessage() {
        // Given
        String messageWithUnicode = "<?xml version=\"1.0\"?><root><element>中文, हिन्दी, العربية, русский, español, français</element></root>";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(messageWithUnicode, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(messageWithUnicode, pubSubOptions.getRequestData());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle very long message")
    void shouldHandleVeryLongMessage() {
        // Given
        StringBuilder longMessage = new StringBuilder("<?xml version=\"1.0\"?><root>");
        for (int i = 0; i < 1000; i++) {
            longMessage.append("<element>This is a very long message that contains a lot of text. </element>");
        }
        longMessage.append("</root>");
        String veryLongMessage = longMessage.toString();
        
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(veryLongMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(veryLongMessage, pubSubOptions.getRequestData());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle special characters in topic")
    void shouldHandleSpecialCharactersInTopic() {
        // Given
        String topicWithSpecialChars = "test-topic-with-special-chars!@#$%^&*()_+-=[]{}|;':\",./<>?";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, topicWithSpecialChars, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(topicWithSpecialChars, pubSubOptions.getTopic());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle special characters in key")
    void shouldHandleSpecialCharactersInKey() {
        // Given
        String keyWithSpecialChars = "test-key-with-special-chars!@#$%^&*()_+-=[]{}|;':\",./<>?";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, keyWithSpecialChars);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertEquals(keyWithSpecialChars, metadata.get("key"));
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle DaprProducer success response")
    void shouldHandleDaprProducerSuccessResponse() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    @DisplayName("Should handle DaprProducer error response")
    void shouldHandleDaprProducerErrorResponse() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.error(new RuntimeException("Kafka error")));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
        // The method should not throw an exception due to onErrorResume handling
    }

    @Test
    @DisplayName("Should handle DaprProducer empty response")
    void shouldHandleDaprProducerEmptyResponse() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.empty());

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    @DisplayName("Should handle multiple consecutive publish calls")
    void shouldHandleMultipleConsecutivePublishCalls() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(3)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    @DisplayName("Should handle different message types")
    void shouldHandleDifferentMessageTypes() {
        // Given
        String jsonMessage = "{\"key\":\"value\",\"number\":123,\"boolean\":true}";
        String plainTextMessage = "This is a plain text message";
        String xmlMessage = "<?xml version=\"1.0\"?><root><element>test</element></root>";
        
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(jsonMessage, testTopic, testKey);
        kafkaUtils.publishToResponseTopic(plainTextMessage, testTopic, testKey);
        kafkaUtils.publishToResponseTopic(xmlMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(3)).invokeDaprPublishEvent(any(PubSubOptions.class));
    }

    @Test
    @DisplayName("Should handle different topic names")
    void shouldHandleDifferentTopicNames() {
        // Given
        String topic1 = "nilrouter";
        String topic2 = "msgeventtracker";
        String topic3 = "error-topic";
        
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, topic1, testKey);
        kafkaUtils.publishToResponseTopic(testMessage, topic2, testKey);
        kafkaUtils.publishToResponseTopic(testMessage, topic3, testKey);

        // Then
        verify(daprProducer, times(3)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertTrue(pubSubOptions.getTopic().equals(topic1) || 
                      pubSubOptions.getTopic().equals(topic2) || 
                      pubSubOptions.getTopic().equals(topic3));
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle different key values")
    void shouldHandleDifferentKeyValues() {
        // Given
        String key1 = "batch-123";
        String key2 = "error";
        String key3 = "transaction-456";
        
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, key1);
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, key2);
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, key3);

        // Then
        verify(daprProducer, times(3)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertTrue(metadata.get("key").equals(key1) || 
                      metadata.get("key").equals(key2) || 
                      metadata.get("key").equals(key3));
            return true;
        }));
    }

    @Test
    @DisplayName("Should verify metadata contains rawPayload flag")
    void shouldVerifyMetadataContainsRawPayloadFlag() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertNotNull(metadata);
            assertEquals("true", metadata.get("rawPayload"));
            return true;
        }));
    }

    @Test
    @DisplayName("Should verify correct DAPR binding name")
    void shouldVerifyCorrectDAPRBindingName() {
        // Given
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(Constants.KAFKA_RESPONSE_TOPIC_DAPR_BINDING, pubSubOptions.getPubsubName());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle whitespace in message")
    void shouldHandleWhitespaceInMessage() {
        // Given
        String messageWithWhitespace = "  <?xml version=\"1.0\"?><root><element>test</element></root>  ";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(messageWithWhitespace, testTopic, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(messageWithWhitespace, pubSubOptions.getRequestData());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle whitespace in topic")
    void shouldHandleWhitespaceInTopic() {
        // Given
        String topicWithWhitespace = "  test-topic  ";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, topicWithWhitespace, testKey);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            assertEquals(topicWithWhitespace, pubSubOptions.getTopic());
            return true;
        }));
    }

    @Test
    @DisplayName("Should handle whitespace in key")
    void shouldHandleWhitespaceInKey() {
        // Given
        String keyWithWhitespace = "  test-key  ";
        when(daprProducer.invokeDaprPublishEvent(any(PubSubOptions.class)))
                .thenReturn(Mono.just("Success"));

        // When
        kafkaUtils.publishToResponseTopic(testMessage, testTopic, keyWithWhitespace);

        // Then
        verify(daprProducer, times(1)).invokeDaprPublishEvent(argThat(pubSubOptions -> {
            Map<String, String> metadata = pubSubOptions.getMetadata();
            assertEquals(keyWithWhitespace, metadata.get("key"));
            return true;
        }));
    }
} 