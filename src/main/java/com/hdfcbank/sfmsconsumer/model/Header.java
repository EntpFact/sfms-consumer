package com.hdfcbank.sfmsconsumer.model;


import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class Header {
    private String msgId;
    private String source;
    private String target;
    private String msgType;
    private String flowType;
    private boolean replayInd;
    private Integer replayCount;
    private boolean invalidPayload;
    private String prefix;
    private String batchId;
    private String batchCreDt;
    private String status;
}
