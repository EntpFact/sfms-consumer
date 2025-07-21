package com.hdfcbank.sfmsconsumer.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MsgEventTracker {

    private String msgId;
    private String source;
    private String target;
    private String flowType;
    private String msgType;
    private String orgnlReq;
    private Integer orgnlReqCount;
    private BigDecimal consolidateAmt;
    private String intermediateReq;
    private Integer intermediateCount;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTimestamp;
}
