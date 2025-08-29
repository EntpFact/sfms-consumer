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
public class AdmiTracker {

    private String msgId;
    private String msgType;
    private String orgnlReq;
    private String target;
    private BigDecimal version;
    private Integer replayCount;
    private String status;
    private Boolean invalidReq;
    private Date batchCreationDate;
    private LocalDateTime batchCreationTimestamp;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTimestamp;

}
