package com.hdfcbank.sfmsconsumer.model;

public enum AuditStatus {
    SUCCESS,        // Inserted successfully
    DUPLICATE,      // Duplicate detected and handled
    ERROR,           // Any other exception occurred
    CAPTURED_DUPLICATE,
    SEND_TO_PROCESSOR_DUPLICATE,
    SEND_TO_DISPATCHER
}