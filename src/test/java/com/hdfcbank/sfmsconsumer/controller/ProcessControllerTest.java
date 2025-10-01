package com.hdfcbank.sfmsconsumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.model.Response;
import com.hdfcbank.sfmsconsumer.service.ErrXmlRoutingService;
import com.hdfcbank.sfmsconsumer.service.IncomingMsgAudit;
import com.hdfcbank.sfmsconsumer.service.PublishMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ProcessControllerTest {

    @Mock
    private PublishMessage publishMessage;

    @Mock
    private IncomingMsgAudit incomingMsgAudit;

    @Mock
    private ErrXmlRoutingService errorMsgAudit;

    @InjectMocks
    private ProcessController controller;

    private String validRequestJson;
    private String invalidRequestJson;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // valid XML
        String xml = "<RequestPayload><AppHdr><BizMsgIdr>RBIP123</BizMsgIdr></AppHdr></RequestPayload>";
        String base64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
        validRequestJson = new ObjectMapper().createObjectNode()
                .put("data_base64", base64)
                .toString();

        // invalid XML (bad string, not parsable as XML)
        String badXml = "%%%%INVALID-XML%%%%";
        String badBase64 = Base64.getEncoder().encodeToString(badXml.getBytes(StandardCharsets.UTF_8));
        invalidRequestJson = new ObjectMapper().createObjectNode()
                .put("data_base64", badBase64)
                .toString();
    }

    @Test
    void testHealthz() {
        ResponseEntity<?> response = controller.healthz();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody());
    }

    @Test
    void testReady() {
        ResponseEntity<?> response = controller.ready();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody());
    }

    @Test
    void testProcess_success() throws JsonProcessingException {
        Mono<ResponseEntity<Response>> responseMono = controller.process(validRequestJson);

        ResponseEntity<Response> response = responseMono.block();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("Message Processed.", response.getBody().getMessage());

        verify(incomingMsgAudit, times(1)).auditIncomingMessage(any(String[].class));
        verify(publishMessage, times(1)).sendRequest(any(String[].class));
        verifyNoInteractions(errorMsgAudit);
    }

/*    @Test
    void testProcess_failure_invalidXml() throws JsonProcessingException {
        Mono<ResponseEntity<Response>> responseMono = controller.process(invalidRequestJson);

        ResponseEntity<Response> response = responseMono.block();
        assertNotNull(response);


        verify(errorMsgAudit, times(1)).determineTopic(eq(invalidRequestJson));
    }*/

    @Test
    void testTestProcess_success() throws JsonProcessingException {
        Mono<ResponseEntity<Response>> responseMono = controller.testProcess(validRequestJson);

        ResponseEntity<Response> response = responseMono.block();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("Message Processed.", response.getBody().getMessage());

        verify(incomingMsgAudit, times(1)).auditIncomingMessage(any(String[].class));
        verify(publishMessage, times(1)).sendRequest(any(String[].class));
        verifyNoInteractions(errorMsgAudit);
    }
}
