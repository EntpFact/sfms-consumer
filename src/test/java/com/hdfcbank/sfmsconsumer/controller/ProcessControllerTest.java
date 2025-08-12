package com.hdfcbank.sfmsconsumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hdfcbank.sfmsconsumer.exception.SFMSException;
import com.hdfcbank.sfmsconsumer.model.Response;
import com.hdfcbank.sfmsconsumer.service.PublishMessage;
import com.hdfcbank.sfmsconsumer.service.XmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessController Tests")
class ProcessControllerTest {

    @Mock
    private PublishMessage publishMessage;

    @InjectMocks
    private ProcessController processController;

    private String validJsonRequest;
    private String validXmlContent;
    private String base64EncodedData;

    @BeforeEach
    void setUp() {
        // Setup test data
        validXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><element>test</element></root>";
        base64EncodedData = Base64.getEncoder().encodeToString(validXmlContent.getBytes());
        validJsonRequest = "{\"data_base64\":\"" + base64EncodedData + "\"}";
    }

    @Test
    @DisplayName("Health check endpoint should return success")
    void healthz_ShouldReturnSuccess() {
        // When
        ResponseEntity<?> response = processController.healthz();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody());
    }

    @Test
    @DisplayName("Ready check endpoint should return success")
    void ready_ShouldReturnSuccess() {
        // When
        ResponseEntity<?> response = processController.ready();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody());
    }

    @Test
    @DisplayName("Process endpoint should return success for valid request")
    void process_WithValidRequest_ShouldReturnSuccess() throws JsonProcessingException {
        // Given
        doNothing().when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.process(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("SUCCESS", body.getStatus());
                    assertEquals("Message Processed.", body.getMessage());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, times(1)).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("Process endpoint should handle PublishMessage exception")
    void process_WhenPublishMessageThrowsException_ShouldReturnError() throws JsonProcessingException {
        // Given
        doThrow(new RuntimeException("Kafka error")).when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.process(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("ERROR", body.getStatus());
                    assertEquals("Message Processing Failed", body.getMessage());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Process endpoint should handle invalid JSON")
    void process_WithInvalidJson_ShouldReturnError() throws JsonProcessingException {
        // Given
        String invalidJson = "invalid json";

        // When
        Mono<ResponseEntity<Response>> result = processController.process(invalidJson);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("ERROR", body.getStatus());
                    assertEquals("Message Processing Failed", body.getMessage());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, never()).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("Process endpoint should handle missing data_base64 field")
    void process_WithMissingBase64Field_ShouldReturnError() throws JsonProcessingException {
        // Given
        String jsonWithoutBase64 = "{\"other_field\":\"value\"}";

        // When
        Mono<ResponseEntity<Response>> result = processController.process(jsonWithoutBase64);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("ERROR", body.getStatus());
                    assertEquals("Message Processing Failed", body.getMessage());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, never()).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("TestProcess endpoint should return success for valid request")
    void testProcess_WithValidRequest_ShouldReturnSuccess() throws JsonProcessingException {
        // Given
        doNothing().when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.testProcess(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("SUCCESS", body.getStatus());
                    assertEquals("Message Processed.", body.getMessage());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, times(1)).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("TestProcess endpoint should handle PublishMessage exception")
    void testProcess_WhenPublishMessageThrowsException_ShouldReturnError() throws JsonProcessingException {
        // Given
        doThrow(new RuntimeException("Kafka error")).when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.testProcess(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("ERROR", body.getStatus());
                    assertEquals("Message Processing Failed", body.getMessage());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("TestProcess endpoint should handle invalid JSON")
    void testProcess_WithInvalidJson_ShouldReturnError() throws JsonProcessingException {
        // Given
        String invalidJson = "invalid json";

        // When
        Mono<ResponseEntity<Response>> result = processController.testProcess(invalidJson);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("ERROR", body.getStatus());
                    assertEquals("Message Processing Failed", body.getMessage());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, never()).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("RemoveBOM should handle null input")
    void removeBOM_WithNullInput_ShouldReturnNull() {
        // When
        String[] result = ProcessController.removeBOM(null);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("RemoveBOM should remove UTF-8 BOM")
    void removeBOM_WithUTF8BOM_ShouldRemoveBOM() {
        // Given
        String xmlWithBOM = "\uFEFF<?xml version=\"1.0\"?><root>test</root>";

        // When
        String[] result = ProcessController.removeBOM(xmlWithBOM);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("", result[0]); // No junk before XML
        assertEquals("<?xml version=\"1.0\"?><root>test</root>", result[1]);
    }

    @Test
    @DisplayName("RemoveBOM should handle escaped characters")
    void removeBOM_WithEscapedCharacters_ShouldUnescape() {
        // Given
        String xmlWithEscapes = "junk\\\\n\\\\r\\\"<?xml version=\"1.0\"?><root>test</root>";

        // When
        String[] result = ProcessController.removeBOM(xmlWithEscapes);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("junk\n\r\"", result[0]);
        assertEquals("<?xml version=\"1.0\"?><root>test</root>", result[1]);
    }

    @Test
    @DisplayName("SplitAtFirstTag should split at first XML tag")
    void splitAtFirstTag_WithValidXML_ShouldSplitCorrectly() {
        // Given
        String message = "junk text<?xml version=\"1.0\"?><root>test</root>";

        // When
        String[] result = ProcessController.splitAtFirstTag(message);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("junk text", result[0]);
        assertEquals("<?xml version=\"1.0\"?><root>test</root>", result[1]);
    }

    @Test
    @DisplayName("SplitAtFirstTag should handle message without XML tags")
    void splitAtFirstTag_WithoutXMLTags_ShouldReturnFullMessageAsJunk() {
        // Given
        String message = "just some text without XML tags";

        // When
        String[] result = ProcessController.splitAtFirstTag(message);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("just some text without XML tags", result[0]);
        assertEquals("", result[1]);
    }

    @Test
    @DisplayName("SplitAtFirstTag should handle empty string")
    void splitAtFirstTag_WithEmptyString_ShouldReturnEmptyArrays() {
        // Given
        String message = "";

        // When
        String[] result = ProcessController.splitAtFirstTag(message);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("", result[0]);
        assertEquals("", result[1]);
    }

    @Test
    @DisplayName("Process endpoint should handle XML with BOM and escaped characters")
    void process_WithXMLContainingBOMAndEscapes_ShouldProcessCorrectly() throws JsonProcessingException {
        // Given
        String xmlWithBOMAndEscapes = "\uFEFFjunk\\\\n\\\\r\\\"<?xml version=\"1.0\"?><root>test</root>";
        String base64WithBOM = Base64.getEncoder().encodeToString(xmlWithBOMAndEscapes.getBytes());
        String jsonWithBOM = "{\"data_base64\":\"" + base64WithBOM + "\"}";
        
        doNothing().when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.process(jsonWithBOM);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("SUCCESS", body.getStatus());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, times(1)).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("Process endpoint should handle malformed XML gracefully")
    void process_WithMalformedXML_ShouldProcessGracefully() throws JsonProcessingException {
        // Given
        String malformedXML = "<root><unclosed>";
        String base64Malformed = Base64.getEncoder().encodeToString(malformedXML.getBytes());
        String jsonWithMalformed = "{\"data_base64\":\"" + base64Malformed + "\"}";
        
        doNothing().when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.process(jsonWithMalformed);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("SUCCESS", body.getStatus());
                    assertEquals("Message Processed.", body.getMessage());
                    return true;
                })
                .verifyComplete();

        // Verify that publishMessage was called even with malformed XML
        verify(publishMessage, times(1)).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("TestProcess endpoint should handle XML sanitization")
    void testProcess_WithXMLSanitization_ShouldProcessCorrectly() throws JsonProcessingException {
        // Given
        doNothing().when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.testProcess(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("SUCCESS", body.getStatus());
                    return true;
                })
                .verifyComplete();

        verify(publishMessage, times(1)).sendRequest(any(String[].class));
    }

    @Test
    @DisplayName("Process endpoint should return error when PublishMessage throws exception with malformed XML")
    void process_WithMalformedXMLAndPublishMessageException_ShouldReturnError() throws JsonProcessingException {
        // Given
        String malformedXML = "<root><unclosed>";
        String base64Malformed = Base64.getEncoder().encodeToString(malformedXML.getBytes());
        String jsonWithMalformed = "{\"data_base64\":\"" + base64Malformed + "\"}";
        
        doThrow(new RuntimeException("Kafka error")).when(publishMessage).sendRequest(any(String[].class));

        // When
        Mono<ResponseEntity<Response>> result = processController.process(jsonWithMalformed);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Response body = response.getBody();
                    assertNotNull(body);
                    assertEquals("ERROR", body.getStatus());
                    assertEquals("Message Processing Failed", body.getMessage());
                    return true;
                })
                .verifyComplete();
    }
} 