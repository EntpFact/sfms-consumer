package com.hdfcbank.sfmsconsumer.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
public class BatchTracker {

    private String batchId;
    private String msgId;
    private String msgType;
    private String status;
    private Integer replayCount;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTimestamp;
}
