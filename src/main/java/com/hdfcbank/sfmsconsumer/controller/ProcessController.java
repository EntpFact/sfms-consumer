package com.hdfcbank.sfmsconsumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.model.Response;
import com.hdfcbank.sfmsconsumer.service.ErrXmlRoutingService;
import com.hdfcbank.sfmsconsumer.service.IncomingMsgAudit;
import com.hdfcbank.sfmsconsumer.service.PublishMessage;
import com.hdfcbank.sfmsconsumer.service.XmlSanitizer;
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

    @CrossOrigin
    @GetMapping(path = "/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("Success");
    }

    @CrossOrigin
    @GetMapping(path = "/ready")
    public ResponseEntity<String> ready() {
        return ResponseEntity.ok("Success");
    }

    @CrossOrigin
    @PostMapping("/process")
    public Mono<ResponseEntity<Response>> process(@RequestBody String request) {
        log.info("....Processing Started....");

        return Mono.fromCallable(() -> validateXml(request))
                .flatMap(xmlString -> {
                    // If XML is valid
                    if (xmlString != null && xmlString.length > 1 && !xmlString[1].isBlank()) {
                        incomingMsgAudit.auditIncomingMessage(xmlString);

                        String sanitized = XmlSanitizer.sanitize(xmlString[1]);
                        xmlString[1] = sanitized;
                        log.info("XmlSanitizer output : {}", sanitized);

                        publishMessage.sendRequest(xmlString);
                    }
                    return Mono.just(ResponseEntity.ok(new Response("SUCCESS", "Message Processed.")));
                })
                .onErrorResume(ex -> {

                    log.error("XML Parsing Failed: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new Response("ERROR", "Message Processing Failed")));
                })
                .doFinally(signalType -> log.info("....Processing Completed...."));
    }


    @CrossOrigin
    @PostMapping("/testProcess")
    public Mono<ResponseEntity<Response>> testProcess(@RequestBody String request) {
        log.info("....Processing Started....");
        return Mono.fromCallable(() -> {
            try {
                log.info("Test request : {}", request);
                String[] xmlString = validateXml(request);

                if (xmlString != null && xmlString.length > 1 && !xmlString[1].isBlank()) {
                    incomingMsgAudit.auditIncomingMessage(xmlString);

                    String sanitized = XmlSanitizer.sanitize(xmlString[1]);
                    xmlString[1] = sanitized;
                    log.info("XmlSanitizer output : {}", sanitized);
                    publishMessage.sendRequest(xmlString);
                }

                return ResponseEntity.ok(new Response("SUCCESS", "Message Processed."));
            } catch (Exception ex) {
                log.error("Failed in consuming the test message: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new Response("ERROR", "Message Processing Failed"));
            } finally {
                log.info("....Processing Completed....");
            }
        }).onErrorResume(ex -> {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response("ERROR", "Message Processing Failed")));
        });
    }

    private String[] validateXml(String request)  {
        String xmlMsg =null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(request);

            String base64Data = rootNode.get("data_base64").asText();
            xmlMsg = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);

            // Remove BOM safely
            String[] xmlMessage = removeBOM(xmlMsg);
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

            builder.parse(new ByteArrayInputStream(xmlMessage[1].getBytes(StandardCharsets.UTF_8)));

            return xmlMessage;
        }catch (SAXException | IOException | ParserConfigurationException e ) {
            // If XML parsing fails
            log.info("Calling determineTopic due to XML error");
            try {
                errorMsgAudit.determineTopic(xmlMsg);
            } catch (Exception dtEx) {
                log.error("determineTopic() failed: {}", dtEx.getMessage(), dtEx);
            };
            return null;
        }

    }


    public static String[] removeBOM(String xml) {
        if (xml == null) return null;
        if (xml.startsWith("\uFEFF")) {
            xml = xml.substring(1);
        }
        return splitAtFirstTag(xml);
    }

    public static String[] splitAtFirstTag(String message) {
        int index = message.indexOf('<');
        if (index == -1) {
            return new String[]{message, ""};
        }
        String before = message.substring(0, index);
        String after = message.substring(index);
        return new String[]{before, after};
    }
}