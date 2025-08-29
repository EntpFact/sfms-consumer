package com.hdfcbank.sfmsconsumer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TargetProcessorTopicConfigTest {

    private TargetProcessorTopicConfig config;

    @BeforeEach
    void setUp() {
        config = new TargetProcessorTopicConfig();
    }

    @Test
    void testSetAndGetProcessor() {
        Map<String, String> processorMap = new HashMap<>();
        processorMap.put("pacs.002.001.09", "NIL");
        processorMap.put("camt.059.001.06", "FC");

        config.setProcessor(processorMap);

        assertNotNull(config.getProcessor());
        assertEquals("NIL", config.getProcessor().get("pacs.002.001.09"));
        assertEquals("FC", config.getProcessor().get("camt.059.001.06"));
    }

    @Test
    void testSetAndGetTopic() {
        Map<String, String> topicMap = new HashMap<>();
        topicMap.put("pacs.002.001.09", "topic-nil");
        topicMap.put("camt.059.001.06", "topic-fc");

        config.setTopic(topicMap);

        assertNotNull(config.getTopic());
        assertEquals("topic-nil", config.getTopic().get("pacs.002.001.09"));
        assertEquals("topic-fc", config.getTopic().get("camt.059.001.06"));
    }

    @Test
    void testGetProcessorFileType_whenPresent() {
        Map<String, String> processorMap = new HashMap<>();
        processorMap.put("pacs.002.001.09", "NIL");
        config.setProcessor(processorMap);

        assertEquals("NIL", config.getProcessorFileType("pacs.002.001.09"));
    }

    @Test
    void testGetProcessorFileType_whenNotPresent() {
        config.setProcessor(new HashMap<>());
        assertNull(config.getProcessorFileType("unknown"));
    }

    @Test
    void testGetProcessorFileType_whenNullMap() {
        config.setProcessor(null);
        assertNull(config.getProcessorFileType("pacs.002.001.09"));
    }

    @Test
    void testGetTopicFileType_whenPresent() {
        Map<String, String> topicMap = new HashMap<>();
        topicMap.put("camt.059.001.06", "topic-fc");
        config.setTopic(topicMap);

        assertEquals("topic-fc", config.getTopicFileType("camt.059.001.06"));
    }

    @Test
    void testGetTopicFileType_whenNotPresent() {
        config.setTopic(new HashMap<>());
        assertNull(config.getTopicFileType("unknown"));
    }

    @Test
    void testGetTopicFileType_whenNullMap() {
        config.setTopic(null);
        assertNull(config.getTopicFileType("pacs.002.001.09"));
    }
}
