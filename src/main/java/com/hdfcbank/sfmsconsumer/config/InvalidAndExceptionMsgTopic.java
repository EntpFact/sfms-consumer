package com.hdfcbank.sfmsconsumer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ Configuration
@ConfigurationProperties(prefix = "kafka")
public class InvalidAndExceptionMsgTopic {

    private String exceptionTopic;
    private String defaultInvalidMsgSwitch;
    private String defaultInvalidMsgTopic;

}