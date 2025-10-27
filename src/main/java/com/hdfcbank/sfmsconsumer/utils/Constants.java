package com.hdfcbank.sfmsconsumer.utils;

public class Constants {
    public static final String KAFKA_RESPONSE_TOPIC_DAPR_BINDING = "kafka-sfmsconsumer-pubsub-component";
    public static final String MSGDEFIDR_XPATH = "//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']";
    public static final String MSGID_XPATH = "//*[local-name()='AppHdr']/*[local-name()='BizMsgIdr']";
    public static final String SFMS = "SFMS";
    public static final String INWARD = "INWARD";
    public static final String CAPTURED = "CAPTURED";
    public static final String RAW_PAYLOAD = "rawPayload";
    public static final String TRUE = "true";
    public static final String ADMI = "admi";
    public static final String BATCH_CREDT_XPATH = "//*[local-name()='AppHdr']/*[local-name()='CreDt']";

}
