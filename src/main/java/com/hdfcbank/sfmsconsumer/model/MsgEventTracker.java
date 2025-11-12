package com.hdfcbank.sfmsconsumer.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MsgEventTracker {

    private String msgId;
    private String source;
    private String target;
    private String flowType;
    private String msgType;
    private String orgnlReq;
    private String batchId;
    private Integer orgnlReqCount;
    private BigDecimal consolidateAmt;
    private String transformedJsonReq;
    private String intermediateReq;
    private Integer intermediateCount;
    private Boolean invalidReq;
    private Boolean bypassEnabled;
    private Integer replayCount;
    private String status;
    private Date batchCreationDate;
    private LocalDateTime batchCreationTimestamp;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTimestamp;
}
