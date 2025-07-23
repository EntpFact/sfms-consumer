package com.hdfcbank.sfmsconsumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.exception.SFMSException;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.model.Response;
import com.hdfcbank.sfmsconsumer.service.DedupCheck;
import com.hdfcbank.sfmsconsumer.service.PublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
public class ProcessController {

    @Autowired
    private PublishMessage publishMessage;

    @Autowired
    private DedupCheck dedupCheck;

    @CrossOrigin
    @GetMapping(path = "/healthz")
    public ResponseEntity<?> healthz() {
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping(path = "/ready")
    public ResponseEntity<?> ready() {
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }


    @CrossOrigin
    @PostMapping("/process")
    public Mono<ResponseEntity<Response>> process(@RequestBody String request) throws JsonProcessingException {
        log.info("....Processing Started.... ");
        return Mono.fromCallable(() -> {
            try {
                // Get base64 encoded data
                String xmlString = validateXml(request);

                // Check duplicate & Audit Incoming message
                //MsgEventTracker msgEventTracker = dedupCheck.checkDuplicate(xmlString);

                // Process the XML message if duplicate is not found
                //if (msgEventTracker == null) {
                    publishMessage.sendRequest(xmlString);
               // }

                return ResponseEntity.ok(new Response("SUCCESS", "Message Processed."));
            } catch (Exception ex) {
                log.error("Failed in consuming the message: {}", ex);
                throw new SFMSException("Failed in consuming the message", ex);
            } finally {
                log.info("....Processing Completed.... ");
            }
        }).onErrorResume(ex -> {
            return Mono.just(new ResponseEntity<>(new Response("ERROR", "Message Processing Failed"), HttpStatus.INTERNAL_SERVER_ERROR));
        });
    }


    private String validateXml(String request) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(request);

        String base64Data = rootNode.get("data_base64").asText();
        String xmlMessage = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);

        // Remove BOM if exists and trim any extra whitespace
        xmlMessage = removeBOM(xmlMessage);
        xmlMessage = xmlMessage.trim();

        // Log the decoded XML message for debugging
        log.info("Decoded XML: {}", xmlMessage);

        // Now you can process the raw XML string
        String xmlString = "";  // String to store the converted XML
        try {
            // Parse the XML string to a Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));

            // Convert Document to string using Transformer
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(new DOMSource(document), result);

            // Assign the XML as a string to the variable
            xmlString = writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            xmlString = "Error processing XML";  // If there's an error, assign an error message
        }
        return xmlString;
    }

    private String removeBOM(String xml) {
        // Check for UTF-8 BOM (EF BB BF)
        if (xml != null && xml.startsWith("\uFEFF")) {
            return xml.substring(1);
        }
        // Sometimes BOM shows up as a junk character like �, remove non-printables too
        return xml.replaceAll("^[^<]+", "");  // Removes any non-XML leading junk
    }

    @CrossOrigin
    @PostMapping("/testProcess")
    public Mono<ResponseEntity<Response>> testProcess(@RequestBody String request) throws JsonProcessingException {

        log.info("....Processing Started.... ");

        return Mono.fromCallable(() -> {
            try {
                // Get base64 encoded data
                //String xmlString = validateXml(request);

                // Check duplicate & Audit Incoming message
                //MsgEventTracker msgEventTracker = dedupCheck.checkDuplicate(request);

                // Process the XML message if duplicate is not found
               // if (msgEventTracker == null) {
                    publishMessage.sendRequest(request);
                //}

                return ResponseEntity.ok(new Response("SUCCESS", "Message Processed."));
            } catch (Exception ex) {
                log.error("Failed in consuming the message: {}", ex);

                throw new SFMSException("Failed in consuming the message", ex);
            } finally {
                log.info("....Processing Completed.... ");
            }
        }).onErrorResume(ex -> {
            return Mono.just(new ResponseEntity<>(new Response("ERROR", "Message Processing Failed"), HttpStatus.INTERNAL_SERVER_ERROR));
        });
    }
}
