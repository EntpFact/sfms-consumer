package com.hdfcbank.sfmsconsumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.model.Response;
import com.hdfcbank.sfmsconsumer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
public class ProcessController {

    @Autowired
    private PublishMessage publishMessage;

    @Autowired
    private IncomingMsgAudit incomingMsgAudit;

    @Autowired
    private ErrXmlRoutingService errorMsgAudit;

    @Autowired
    private BypassService bypassService;

    @CrossOrigin
    @GetMapping("/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("Success");
    }

    @CrossOrigin
    @GetMapping("/ready")
    public ResponseEntity<String> ready() {
        return ResponseEntity.ok("Success");
    }

    @CrossOrigin
    @PostMapping("/process")
    public Mono<ResponseEntity<Response>> process(@RequestBody String request) {
        log.info("....Processing Started....");

        return Mono.fromCallable(() -> validateXml(request))
                .flatMap(xmlString -> {

                    //  Bypass strategy check
                    if (bypassService.isBypassEnabled()) {
                        log.info("Bypass strategy enabled — routing message to configured switch/topic");
                        bypassService.sendToBypassSwitch(xmlString);
                        return Mono.just(ResponseEntity.ok(
                                new Response("SUCCESS", "Message routed via bypass.")));
                    }

                    // 2️ Normal processing flow
                    return incomingMsgAudit.auditIncomingMessage(xmlString)
                            .flatMap(status -> {
                                switch (status) {
                                    case SUCCESS:
                                        // Continue for non-duplicate messages
                                        String sanitized = XmlSanitizer.sanitize(xmlString[1]);
                                        xmlString[1] = sanitized;
                                        log.info("XmlSanitizer output: {}", sanitized);

                                        return publishMessage.sendRequest(xmlString)
                                                .thenReturn(ResponseEntity.ok(
                                                        new Response("SUCCESS", "Message Processed.")));
                                    case DUPLICATE:
                                        log.warn("Duplicate message detected — skipping further processing.");
                                        return Mono.just(ResponseEntity.ok(
                                                new Response("DUPLICATE", "Message already processed.")));
                                    case ERROR:
                                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(new Response("ERROR", "Message processing failed.")));
                                    default:
                                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(new Response("ERROR", "Message audit failed.")));
                                }


                            });
                })
                .onErrorResume(ex -> {
                    log.error("XML Parsing Failed: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new Response("ERROR", "Message Processing Failed")));
                })
                .doFinally(signal -> log.info("....Processing Completed...."));
    }

    // For testing bypass or flow logic manually
    @CrossOrigin
    @PostMapping("/testProcess")
    public Mono<ResponseEntity<Response>> testProcess(@RequestBody String request) {
        log.info("....Processing Started....");
        return Mono.fromCallable(() -> validateXml(request))
                .flatMap(xmlString -> {

                    //  Bypass strategy check
                    if (bypassService.isBypassEnabled()) {
                        log.info("Bypass strategy enabled — routing message to configured switch/topic");
                        bypassService.sendToBypassSwitch(xmlString);
                        return Mono.just(ResponseEntity.ok(
                                new Response("SUCCESS", "Message routed via bypass.")));
                    }

                    // 2️ Normal processing flow
                    return incomingMsgAudit.auditIncomingMessage(xmlString)
                            .flatMap(status -> {
                                switch (status) {
                                    case SUCCESS:
                                        // Continue for non-duplicate messages
                                        String sanitized = XmlSanitizer.sanitize(xmlString[1]);
                                        xmlString[1] = sanitized;
                                        log.info("XmlSanitizer output: {}", sanitized);

                                        return publishMessage.sendRequest(xmlString)
                                                .thenReturn(ResponseEntity.ok(
                                                        new Response("SUCCESS", "Message Processed.")));
                                    case DUPLICATE:
                                        log.warn("Duplicate message detected — skipping further processing.");
                                        return Mono.just(ResponseEntity.ok(
                                                new Response("DUPLICATE", "Message already processed.")));
                                    case ERROR:
                                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(new Response("ERROR", "Message processing failed.")));
                                    default:
                                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(new Response("ERROR", "Message audit failed.")));
                                }


                            });
                })
                .onErrorResume(ex -> {
                    log.error("XML Parsing Failed: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new Response("ERROR", "Message Processing Failed")));
                })
                .doFinally(signal -> log.info("....Processing Completed...."));
    }

    //  XML Validation + Error Routing
    private String[] validateXml(String request) {
        String xmlMsg = null;
        try {
 /*           ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(request);

            String base64Data = rootNode.get("data_base64").asText();
            xmlMsg = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
*/
            // Remove BOM safely
            String[] xmlMessage = removeBOM(request);
            xmlMessage[0] = xmlMessage[0].trim();
            xmlMessage[1] = xmlMessage[1].trim();

            // Parse XML securely
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException { throw e; }
                @Override
                public void fatalError(SAXParseException e) throws SAXException { throw e; }
                @Override
                public void warning(SAXParseException e) throws SAXException { throw e; }
            });

            Document document = builder.parse(new ByteArrayInputStream(xmlMessage[1].getBytes(StandardCharsets.UTF_8)));
            return xmlMessage;

        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.info("Calling determineTopic due to XML error");
            errorMsgAudit.determineTopic(request).subscribe(
                    nullValue -> {}, // onNext, not used for Mono<Void>
                    err -> log.error("Error in determineTopic", err),
                    () -> log.info("determineTopic completed successfully")
            );
            return null;
        }
    }

    //  Utility: Remove BOM and split header/body
    public static String[] removeBOM(String xml) {
        if (xml == null) return null;
        if (xml.startsWith("\uFEFF")) xml = xml.substring(1);
        return splitAtFirstTag(xml);
    }

    public static String[] splitAtFirstTag(String message) {
        int index = message.indexOf('<');
        if (index == -1) return new String[]{message, ""};
        String before = message.substring(0, index);
        String after = message.substring(index);
        return new String[]{before, after};
    }
}
