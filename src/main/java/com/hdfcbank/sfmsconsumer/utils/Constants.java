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
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String JSON_PROCESSING_ERROR = "JSON_PROCESSING_ERROR";
    public static final String XPATH_PARSING_ERROR = "XPATH_PARSING_ERROR";
    public static final String SFMS_CONSUMER = "sfms-consumer";
    public static final String DAPR_PUBSUB_COMPONENT = "kafka-sfmsconsumer-pubsub-component";
    public static final String SERVICE_NAME = "NEFT_IL_SFMS_CONSUMER";
    public static final String CLOUD_EVENT_TYPE = "custom-event-type";
    public static final String CLOUD_EVENT_SPEC_VERSION = "1.0";
    public static final String CLOUD_EVENT_CONTENT_TYPE = "application/cloudevents+json";

    public static final String PAYLOAD_PRE_PROCESSING_EXCEPTION="Payload Pre-Processing Exception";
    public static final String PAYLOAD_POST_PROCESSING_EXCEPTION="Payload Post-Processing Exception";
    public static final String TECX="TECX";
}
