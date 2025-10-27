package com.hdfcbank.sfmsconsumer.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.*;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;


@Slf4j
@Service
@AllArgsConstructor
public class ErrXmlRoutingService {

    @Autowired
    private KafkaUtils kafkaUtils;

    @Autowired
    private SFMSConsumerRepository sfmsConsumerRepository;

    private final TargetProcessorTopicConfig config;

    /**
     * Determine the topic based on keyword matches in raw XML.
     */
    public Mono<Void> determineTopic(String xmlMessage) {
        try {
            log.info("Inside determineTopic : message {}", xmlMessage);
            String lowerMsg = xmlMessage.toLowerCase();

            Optional<Map.Entry<String, String>> targetProcessor = config.getProcessor().entrySet().stream()
                    .filter(e -> lowerMsg.contains(e.getKey().toLowerCase()))
                    .findFirst();

            if (targetProcessor.isEmpty()) {
                log.warn("No matching processor found for this XML");
                return Mono.empty();
            }

            String target = targetProcessor.map(Map.Entry::getValue).orElse(null);
            String msgType = targetProcessor.get().getKey();
            String topic = config.getTopicFileType(target);

            ReqPayload reqPayload = setReqPayloadFields(xmlMessage, msgType, target);
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqPayload);

            if (msgType.toLowerCase().contains("admi")) {
                return admiErrorMessageAudit(xmlMessage, msgType, json)
                        .flatMap(status -> {
                            kafkaUtils.publishToResponseTopic(json, topic);
                            return Mono.empty();
                        });
            } else {
                return mvtErrorMessageAudit(xmlMessage, msgType, json)
                        .flatMap(status -> {
                            kafkaUtils.publishToResponseTopic(json, topic);
                            return Mono.empty();
                        });
            }

        } catch (JsonProcessingException e) {
            log.error("Error generating JSON for error routing", e);
            return Mono.error(e);
        }
    }

    /**
     * Save MVT error message and handle duplicates by incrementing version.
     */
    public Mono<AuditStatus> mvtErrorMessageAudit(String errorReq, String msgType, String reqPayload) {
        String target = config.getProcessorFileType(msgType.trim());
        String batchId = extractBatchIdValue(errorReq);
        MsgEventTracker tracker = MsgEventTracker.builder()
                .msgId(getMsgId(errorReq))
                .orgnlReq(errorReq)
                .msgType(msgType)
                .flowType("INWARD")
                .source("SFMS")
                .batchId(batchId)
                .target(target)
                .invalidReq(true)
                .build();

        return sfmsConsumerRepository.saveDataInMsgEventTracker(tracker)
                .flatMap(status -> {
                    if (status == AuditStatus.DUPLICATE) {
                        log.warn("Duplicate entry detected for msgId {} — incrementing version", tracker.getMsgId());
                        return sfmsConsumerRepository.incrementMsgEventTrackerVersion(tracker.getMsgId())
                                .thenReturn(AuditStatus.DUPLICATE);
                    }
                    return Mono.just(status);
                })
                .onErrorResume(ex -> {
                    log.error("Error saving MsgEventTracker for msgId {}: {}", tracker.getMsgId(), ex.getMessage(), ex);
                    return Mono.just(AuditStatus.ERROR);
                });
    }

    /**
     * Save ADMI error message and handle duplicates by incrementing version.
     */
    public Mono<AuditStatus> admiErrorMessageAudit(String errorReq, String msgType, String reqPayload) {
        String target = config.getProcessorFileType(msgType.trim());
        AdmiTracker admiTracker = AdmiTracker.builder()
                .msgId(getMsgId(errorReq))
                .msgType(msgType)
                .target(target)
                .invalidReq(true)
                .orgnlReq(errorReq)
                .build();

        return sfmsConsumerRepository.saveDataInAdmiTracker(admiTracker)
                .flatMap(status -> {
                    if (status == AuditStatus.DUPLICATE) {
                        log.warn("Duplicate entry detected for msgId {} — incrementing version", admiTracker.getMsgId());
                        return sfmsConsumerRepository.incrementAdmiTrackerVersion(admiTracker.getMsgId())
                                .thenReturn(AuditStatus.DUPLICATE);
                    }
                    return Mono.just(status);
                })
                .onErrorResume(ex -> {
                    log.error("Error saving AdmiTracker for msgId {}: {}", admiTracker.getMsgId(), ex.getMessage(), ex);
                    return Mono.just(AuditStatus.ERROR);
                });
    }

    private ReqPayload setReqPayloadFields(String xmlMessage, String msgType, String targetProcessor) {
        String msgId = getMsgId(xmlMessage);
        Header header = Header.builder()
                .msgId(msgId)
                .msgType(msgType)
                .source("SFMS")
                .target(targetProcessor)
                .invalidPayload(true)
                .flowType("INWARD")
                .replayInd(false)
                .prefix(null)
                .build();

        Body body = Body.builder()
                .payload(xmlMessage)
                .build();

        return ReqPayload.builder()
                .header(header)
                .body(body)
                .build();
    }

    public String getMsgId(String xmlMessage) {
        Pattern pattern = Pattern.compile("<\\s*BizMsgIdr\\s*>(.*?)<\\s*/\\s*BizMsgIdr\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xmlMessage);
        if (matcher.find()) return matcher.group(1);
        log.error("MsgId not found in XML");
        return null;
    }

    public String extractBatchIdValue(String xml) {
        Pattern pattern = Pattern.compile("<(Ustrd|AddtlInf)>(.*?)</\\1>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) return matcher.group(2);
        return null;
    }
}

//
//public class ErrXmlRoutingService {
//
//    @Autowired
//    KafkaUtils kafkaUtils;
//
//    @Autowired
//    private SFMSConsumerRepository SFMSConsumerRepository;
//
//    private final TargetProcessorTopicConfig config;
//
//
//    /**
//     * Determine the topic based on keyword matches in raw XML.
//     */
//    public void determineTopic(String xmlMessage) {
//        try {
//            log.info("inside determineTopic : message {}",xmlMessage);
//            String lowerMsg = xmlMessage.toLowerCase();
//
//            // Get target processor
//            Optional<Map.Entry<String, String>> targetProcessor = config.getProcessor().entrySet().stream()
//                    .filter(e -> lowerMsg.contains(e.getKey().toLowerCase()))
//                    .findFirst();
//
//            log.info("Processor config keys: {}", config.getProcessor().keySet());
//            if (targetProcessor.isPresent()) {
//                // Get target processor
//                String target = targetProcessor.map(Map.Entry::getValue).orElse(null);
//                // Get Msg Type
//                String msgType = targetProcessor.get().getKey();
//                // Get topic
//                String topic = config.getTopicFileType(targetProcessor.get().getValue());
//                ReqPayload reqPayload = setReqPayloadFields(xmlMessage, msgType, target);
//
//                ObjectMapper mapper = new ObjectMapper();
//                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqPayload);
//                if (msgType.contains(ADMI)) {
//                    admiErrorMessageAudit(xmlMessage, msgType, json);
//                } else {
//                    mvtErrorMessageAudit(xmlMessage, msgType, json);
//                }
//
//                log.info("Error Json : {}", json);
//                kafkaUtils.publishToResponseTopic(json, topic);
//            } else {
//            log.warn("No matching processor found for this XML");
//        }
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void mvtErrorMessageAudit(String errorReq, String msgType, String reqPayload) {
//        String target = config.getProcessorFileType(msgType.trim());
//        String batchId = extractBatchIdValue(errorReq);
//        MsgEventTracker tracker = MsgEventTracker.builder()
//                .msgId(getMsgId(errorReq))
//                .orgnlReq(errorReq)
//                .msgType(msgType)
//                .flowType(INWARD)
//                .source(SFMS)
//                .batchId(batchId)
//                //.transformedJsonReq(reqPayload)
//                .target(target)
//                .invalidReq(true)
//                .build();
//
//        // Save to error msg_event_tracker table
//        SFMSConsumerRepository.saveDataInMsgEventTracker(tracker);
//    }
//
//    public void admiErrorMessageAudit(String errorReq, String msgType, String reqPayload) {
//
//        String target = config.getProcessorFileType(msgType.trim());
//        AdmiTracker admiTracker = AdmiTracker.builder()
//                .msgId(getMsgId(errorReq))
//                .msgType(msgType)
//                .target(target)
//               // .transformedJsonReq(reqPayload)
//                .invalidReq(true)
//                .orgnlReq(errorReq)
//                .build();
//
//        // Save to error admi004_tracker table
//        SFMSConsumerRepository.saveDataInAdmiTracker(admiTracker);
//
//    }
//
//
//    private ReqPayload setReqPayloadFields(String xmlMessage, String msgType, String targetProcessor) {
//        String msgId = getMsgId(xmlMessage);
//        Header header = Header.builder()
//                .msgId(msgId)
//                .msgType(msgType)
//                .source(SFMS)
//                .target(targetProcessor)
//                .invalidPayload(true)
//                .flowType(INWARD)
//                .replayInd(false)
//                .prefix(null)
//                .build();
//
//        Body body = Body.builder()
//                .payload(xmlMessage)
//                .build();
//
//        return ReqPayload.builder()
//                .header(header)
//                .body(body)
//                .build();
//    }
//
//
//    public String getMsgId(String xmlMessage) {
//
//        Pattern pattern = Pattern.compile("<\\s*BizMsgIdr\\s*>(.*?)<\\s*/\\s*BizMsgIdr\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
//        Matcher matcher = pattern.matcher(xmlMessage);
//        String msgId = null;
//        if (matcher.find()) {
//            msgId = matcher.group(1);
//            log.info("Error Message - MsgId: " + msgId);
//        } else {
//            log.error("Error Message - MsgId not found.");
//        }
//        return msgId;
//    }
//
//    public String extractBatchIdValue(String xml) {
//        // Regex will match either <Ustrd>...</Ustrd> or <AddtlInf>...</AddtlInf>
//        String regex = "<(Ustrd|AddtlInf)>(.*?)</\\1>";
//        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
//        Matcher matcher = pattern.matcher(xml);
//
//        if (matcher.find()) {
//            return matcher.group(2); // the inner value of first match
//        }
//        return null; // if neither tag is present
//    }
//
//
//}
