package com.hdfcbank.sfmsconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "file-type")
public class TargetProcessorTopicConfig {

    private Map<String, String> processor;

    private Map<String, String> topic;

    public Map<String, String> getTopic() {
        return topic;
    }

    public void setTopic(Map<String, String> topic) {
        this.topic = topic;
    }

    public Map<String, String> getProcessor() {
        return processor;
    }

    public void setProcessor(Map<String, String> processor) {
        this.processor = processor;
    }

    public String getProcessorFileType(String fileType) {
        if (processor == null) {
            return null;
        }
        return processor.get(fileType);
    }

    public String getTopicFileType(String fileType) {
        if (topic == null) {
            return null;
        }
        return topic.get(fileType);
    }
}