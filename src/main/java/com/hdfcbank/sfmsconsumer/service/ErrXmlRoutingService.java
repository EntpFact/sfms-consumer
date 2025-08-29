package com.hdfcbank.sfmsconsumer.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.NilRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    KafkaUtils kafkaUtils;

    @Autowired
    private NilRepository nilRepository;

    private final TargetProcessorTopicConfig config;


    /**
     * Determine the topic based on keyword matches in raw XML.
     */
    public void determineTopic(String xmlMessage) {
        String lowerMsg = xmlMessage.toLowerCase();
        // 1. Check pacs008, pacs004, pacs002 message types (first matching key wins)
        Optional<Map.Entry<String, String>> resolvedTopic = config.getTopic().entrySet().stream()
                .filter(e -> lowerMsg.contains(e.getKey()))
                .findFirst();

        // 2. Get target processor
        Optional<Map.Entry<String, String>> targetProcessor = config.getProcessor().entrySet().stream()
                .filter(e -> lowerMsg.contains(e.getKey()))
                .findFirst();


        if (resolvedTopic.isPresent()) {
            String target = targetProcessor.map(Map.Entry::getValue).orElse(null);
            String msgType = resolvedTopic.get().getKey();
            ReqPayload reqPayload = setReqPayloadFields(xmlMessage, msgType, target);

            if (msgType.contains(ADMI)) {
                admiErrorMessageAudit(xmlMessage, msgType);
            } else {
                errorMessageAudit(xmlMessage, msgType);
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqPayload);
                log.info("Error Json : {}", json);
                kafkaUtils.publishToResponseTopic(json, resolvedTopic.get().getValue());

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void errorMessageAudit(String errorReq, String msgType) {
        MsgEventTracker tracker = new MsgEventTracker();
        String target = config.getProcessorFileType(msgType.trim());
        tracker.setMsgId(getMsgId(errorReq));
        tracker.setOrgnlReq(errorReq);
        tracker.setMsgType(msgType);
        tracker.setFlowType(INWARD);
        tracker.setSource(SFMS);
        tracker.setTarget(target);
        tracker.setInvalidReq(true);

        // Save to error msg_event_tracker table
        nilRepository.saveDataInMsgEventTracker(tracker);
    }

    public void admiErrorMessageAudit(String errorReq, String msgType) {

        String target = config.getProcessorFileType(msgType.trim());
        AdmiTracker admiTracker = AdmiTracker.builder()
                .msgId(getMsgId(errorReq))
                .msgType(msgType)
                .target(target)
                .invalidReq(true)
                .orgnlReq(errorReq)
                .build();

        // Save to error admi004_tracker table
        nilRepository.saveDataInAdmiTracker(admiTracker);

    }


    private ReqPayload setReqPayloadFields(String xmlMessage, String msgType, String targetProcessor) {
        String msgId = getMsgId(xmlMessage);
        Header header = Header.builder()
                .msgId(msgId)
                .msgType(msgType)
                .source(SFMS)
                .target(targetProcessor)
                .invalidPayload(true)
                .flowType(INWARD)
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


    private String getMsgId(String xmlMessage) {

        Pattern pattern = Pattern.compile("<\\s*BizMsgIdr\\s*>(.*?)<\\s*/\\s*BizMsgIdr\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xmlMessage);
        String msgId = null;
        if (matcher.find()) {
            msgId = matcher.group(1);
            log.info("Error Message - MsgId: " + msgId);
        } else {
            log.error("Error Message - MsgId not found.");
        }
        return msgId;
    }


}
