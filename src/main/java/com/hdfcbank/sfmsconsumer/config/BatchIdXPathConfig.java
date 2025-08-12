package com.hdfcbank.sfmsconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "file-type")
public class BatchIdXPathConfig {

    private Map<String, String> xpaths;

    public Map<String, String> getXpaths() {
        return xpaths;
    }

    public void setXpaths(Map<String, String> xpaths) {
        this.xpaths = xpaths;
    }

    public String getXPathForFileType(String fileType) {
        if (xpaths == null) {
            return null;
        }
        return xpaths.get(fileType);
    }
}
