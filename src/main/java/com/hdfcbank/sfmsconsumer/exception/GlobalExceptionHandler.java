package com.hdfcbank.sfmsconsumer.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hdfcbank.sfmsconsumer.config.InvalidAndExceptionMsgTopic;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.ExceptionDetail;
import com.hdfcbank.sfmsconsumer.model.ReqPayload;
import com.hdfcbank.sfmsconsumer.model.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.hdfcbank.sfmsconsumer.utils.Constants.PAYLOAD_PRE_PROCESSING_EXCEPTION;
import static com.hdfcbank.sfmsconsumer.utils.Constants.TECX;


@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    ObjectMapper mapper;
    KafkaUtils kafkaUtils;
    SFMSConsumerRepository dao;
    InvalidAndExceptionMsgTopic config;

    @ExceptionHandler({SFMSConsumerException.DBException.class})
    public ResponseEntity<Object> handleCustomException(SFMSConsumerException.DBException ex) throws JsonProcessingException {
        String message = ex.getMessage();
        String errorCode;
        ReqPayload payload = prepareExceptionPayload(ex);
        var exceptionPayloadString = mapper.writeValueAsString(payload);
        try {
            // Extract sangamErrorCode from the message JSON
            log.info("In the global exception handler - try block");
        } catch (Exception e) {
            log.info("In the global exception handler - catch block");
        }
        return ResponseEntity.status(HttpStatus.OK).body(message);
    }

    @ExceptionHandler({SFMSConsumerException.NILException.class})
    public ResponseEntity<Object> handleCustomException(SFMSConsumerException.NILException ex) throws JsonProcessingException {
        String message = ex.getMessage();
        ReqPayload payload = prepareExceptionPayload(ex);
        var exceptionPayloadString = mapper.writeValueAsString(payload);
        try {
            if (message.contains(PAYLOAD_PRE_PROCESSING_EXCEPTION)) {
              //  dao.updateMsgEventTrackerStatusForException(payload); //to do
                kafkaUtils.publishToKafkaTopic(exceptionPayloadString, config.getExceptionTopic(), payload.getHeader().getMsgId());
            } else if (message.contains(PAYLOAD_PRE_PROCESSING_EXCEPTION)) {
               // dao.updateMsgEventTrackerStatusForException(payload); //to do
            }
            kafkaUtils.publishToKafkaTopic(exceptionPayloadString, config.getExceptionTopic(), payload.getHeader().getMsgId());
            return ResponseEntity.status(HttpStatus.OK).body(message);
        } catch (Exception e) {
            log.info("Exception occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Response("ERROR", "Message Processing Failed"));
        }
    }

    public ReqPayload prepareExceptionPayload(SFMSConsumerException.NILException ex) throws JsonProcessingException {
        ReqPayload payload = (ex.getReqPayload() != null) ? ex.getReqPayload() :
                (ex.getRequest() != null) ?
                        (mapper.readValue(ex.getRequest(), ReqPayload.class)) : new ReqPayload();
        payload.getHeader().setStatus(TECX);
        var exceptionDetails = ExceptionDetail.builder().exceptionType(ex.getMessage()).exceptionDesc(ex.getCause().toString()).build();
        payload.setExceptionDetail(exceptionDetails);
        return payload;
    }

    public ReqPayload prepareExceptionPayload(SFMSConsumerException.DBException ex) throws JsonProcessingException {
        ReqPayload payload = (ex.getReqPayload() != null) ? ex.getReqPayload() :
                (ex.getRequest() != null) ?
                        (mapper.readValue(ex.getRequest(), ReqPayload.class)) : new ReqPayload();
        payload.getHeader().setStatus(TECX);
        var exceptionDetails = ExceptionDetail.builder().exceptionType(ex.getMessage()).exceptionDesc(ex.getCause().toString()).build();
        payload.setExceptionDetail(exceptionDetails);
        return payload;
    }
}
