package com.hdfcbank.sfmsconsumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.exception.SFMSException;
import com.hdfcbank.sfmsconsumer.model.Response;
import com.hdfcbank.sfmsconsumer.service.PublishMessage;
import com.hdfcbank.sfmsconsumer.service.XmlSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
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
                String xmlString[] = validateXml(request);

                publishMessage.sendRequest(xmlString);


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


    private String[] validateXml(String request) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(request);

        String base64Data = rootNode.get("data_base64").asText();
        String xmlMsg = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);

        String xmlMessage[] = null;
        // Remove BOM if exists and trim any extra whitespace
        xmlMessage = removeBOM(xmlMsg);
        xmlMessage[0] = xmlMessage[0].trim();
        xmlMessage[1] = xmlMessage[1].trim();

        // Log the decoded XML message for debugging
        log.info("Decoded XML: {}", xmlMessage);

        // Now you can process the raw XML string
        String xmlString = "";  // String to store the converted XML
        try {
            // Parse the XML string to a Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlMessage[1].getBytes()));

            // Convert Document to string using Transformer
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(new DOMSource(document), result);

            // Assign the XML as a string to the variable
            xmlString = writer.toString();
            xmlMessage[1]=xmlString;
        } catch (Exception e) {
            e.printStackTrace();
            xmlString = "Error processing XML";  // If there's an error, assign an error message
        }
        return xmlMessage;
    }

    public static String[] removeBOM(String xml) {
        if (xml == null) return null;

        // 1. Remove UTF-8 BOM if present
        if (xml.startsWith("\uFEFF")) {
            xml = xml.substring(1);
        }

        // 2. Only unescape if clearly escaped (Java-style)
        if (xml.contains("\\\\n") || xml.contains("\\\\r") || xml.contains("\\\"")) {
            xml = xml
                    .replace("\\\\n", "\n")
                    .replace("\\\\r", "\r")
                    .replace("\\\"", "\"");
        }

        String[] parts = splitAtFirstTag(xml);

        return parts;
    }

    /**
     * Splits the input string into two parts:
     * 1. Junk before the first `<`
     * 2. Valid XML starting with `<`
     */
    public static String[] splitAtFirstTag(String message) {
        int index = message.indexOf('<');
        if (index == -1) {
            // No '<' found, return full message as junk, no XML
            return new String[] { message, "" };
        }
        String before = message.substring(0, index);
        String after = message.substring(index);
        return new String[] { before, after };
    }

    @CrossOrigin
    @PostMapping("/testProcess")
    public Mono<ResponseEntity<Response>> testProcess(@RequestBody String request) throws JsonProcessingException {

        log.info("....Processing Started.... ");

        return Mono.fromCallable(() -> {
            try {
                //log.info("Request : {}",request);
                // Get base64 encoded data
                String xmlString[] = validateXml(request);
                String sanitizeReq = XmlSanitizer.sanitize(xmlString[1]);
                xmlString[1]=sanitizeReq;
                log.info("XmlSanitizer : {}", sanitizeReq);

                publishMessage.sendRequest(xmlString);


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
