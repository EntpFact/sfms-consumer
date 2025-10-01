package com.hdfcbank.sfmsconsumer.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "batch-tracker")
public class BTAllowedMsgType {

    private List<String> allowedMsgTypes = new ArrayList<>();

    public List<String> getAllowedMsgTypes() {
        return allowedMsgTypes;
    }

    public void setAllowedMsgTypes(List<String> allowedMsgTypes) {
        this.allowedMsgTypes = allowedMsgTypes;
    }
}
