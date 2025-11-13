package com.hdfcbank.sfmsconsumer.service;


import com.hdfcbank.sfmsconsumer.config.BypassProperties;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.AuditStatus;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;

@Service
@AllArgsConstructor
public class BypassService {

    private static final Logger log = LoggerFactory.getLogger(BypassService.class);

    private KafkaUtils kafkaUtils;
    private final BypassProperties bypassProperties;


    @Autowired
    private final SFMSConsumerRepository sfmsConsumerRepository;

    @Autowired
    private final SfmsConsmrCommonUtility sfmsConsmrCommonUtility;


    @Autowired
    private final BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @Autowired
    private final BuildJsonReq buildJsonReq;


    public boolean isBypassEnabled() {
        return bypassProperties.isEnabled();
    }

    public void sendToBypassSwitch(String xml[]) {

        String topic = null;
        String msgId = null;
        String target = null;
        String msgType = null;
        String batchId = null;
        try {
            String switchName = bypassProperties.getDefaultSwitch();
            if (switchName == null || switchName.isBlank()) {
                throw new IllegalStateException("No default switch configured in application.yml");
            }

            topic = bypassProperties.getTopicForSwitch(switchName);
            if (topic == null) {
                throw new IllegalArgumentException("No topic configured for switch: " + switchName);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = null;

            builder = factory.newDocumentBuilder();

            Document document = builder.parse(new InputSource(new StringReader(xml[1])));

            msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
            msgType = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
            target = bypassProperties.getDefaultSwitch();
            batchId = batchIdXmlFieldExtractor.extractFieldByFileType(xml[1]);
            String batchDateTime = sfmsConsmrCommonUtility.getValueByXPath(document, BATCH_CREDT_XPATH);
            Instant instant = Instant.parse(batchDateTime);
            LocalDateTime localDateTime = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();


            log.info("Bypass enabled — sending message to switch '{}' (topic '{}')", switchName, topic);
            kafkaUtils.publishToKafkaTopic(topic, xml[0] + xml[1], msgId);
            Mono<AuditStatus> auditStatusMono = msgType.contains(ADMI)
                    ? handleAdmiTracker(xml, msgId, msgType, target, localDateTime, topic)
                    : handleMsgEventTracker(xml, msgId, msgType, target, batchId, localDateTime, topic);

            auditStatusMono.subscribe(status -> {
                log.info("Insert result: {}", status);
            }, error -> {
                log.error("Error in insert", error);
            });

        } catch (Exception e) {

            saveInvalidByPassMsgAndSendToKafka(xml, msgId, topic, msgType, target,null);

        }

    }


    private Mono<AuditStatus> handleAdmiTracker(String[] xmlMessage, String msgId, String msgType,
                                                String target, LocalDateTime batchDateTime, String topic) {

        AdmiTracker admiTracker = AdmiTracker.builder()
                .msgId(msgId)
                .msgType(msgType)
                .target(target)
                .invalidReq(false)
                .bypassEnabled(true)
                .orgnlReq(xmlMessage[0] + xmlMessage[1])
                .batchCreationTimestamp(batchDateTime)
                .build();

        return sfmsConsumerRepository.saveDataInAdmiTrackerByPassEnabled(admiTracker)
                .flatMap(status -> {
                    if (status == AuditStatus.DUPLICATE) {
                        log.warn("Duplicate entry for msgId: {} — updating version", msgId);
                        return sfmsConsumerRepository.incrementAdmiTrackerVersion(msgId)
                                .thenReturn(AuditStatus.DUPLICATE);
                    }
                    return Mono.just(status);
                })
                .onErrorResume(ex -> {

                    String errMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

                    boolean isDbDown =
                            ex instanceof R2dbcNonTransientResourceException ||
                                    ex instanceof R2dbcTransientResourceException ||
                                    errMsg.contains("Connection refused") ||
                                    errMsg.contains("timeout") ||
                                    errMsg.contains("connection reset") ||
                                    errMsg.contains("Database is unavailable");

                    if (isDbDown) {
                        // Database down → send original XML to negative ACK topic
                        log.error(" Database DOWN detected for msgId {}: {}", msgId, errMsg);
                        return Mono.just(AuditStatus.ERROR);
                    }

                    log.error("Error while saving AdmiTracker for msgId {}: {}", msgId, ex.getMessage(), ex);
                    log.error("Routing failed message to exception topic due to error");
                    saveInvalidByPassMsgAndSendToKafka(xmlMessage, msgId, topic, msgType, target,null);
                    return Mono.just(AuditStatus.ERROR);
                });
    }

    private Mono<AuditStatus> handleMsgEventTracker(String[] xmlMessage, String msgId, String msgType,
                                                    String target, String batchId, LocalDateTime batchDateTime, String topic) {

        MsgEventTracker msgEventTracker = MsgEventTracker.builder()
                .msgId(msgId)
                .msgType(msgType)
                .source(SFMS)
                .flowType(INWARD)
                .invalidReq(false)
                .target(target)
                .bypassEnabled(true)
                .batchId(batchId != null ? batchId : "")
                .orgnlReq(xmlMessage[0] + xmlMessage[1])
                .batchCreationTimestamp(batchDateTime)
                .build();

        return sfmsConsumerRepository.saveDataInMsgEventTrackerByPassEnabled(msgEventTracker)
                .flatMap(status -> {
                    if (status == AuditStatus.DUPLICATE) {
                        log.warn("Duplicate entry for msgId: {} — updating version", msgId);
                        return sfmsConsumerRepository.incrementMsgEventTrackerVersion(msgId)
                                .thenReturn(AuditStatus.DUPLICATE);
                    }
                    return Mono.just(status);
                })
                .onErrorResume(ex -> {


                    String errMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

                    boolean isDbDown =
                            ex instanceof R2dbcNonTransientResourceException ||
                                    ex instanceof R2dbcTransientResourceException ||
                                    errMsg.contains("Connection refused") ||
                                    errMsg.contains("timeout") ||
                                    errMsg.contains("connection reset") ||
                                    errMsg.contains("Database is unavailable");

                    if (isDbDown) {
                        // Database down → send original XML to negative ACK topic
                        log.error(" Database DOWN detected for msgId {}: {}", msgId, errMsg);
                        return Mono.just(AuditStatus.ERROR);
                    }

                    log.error("Error while saving MsgEventTracker for msgId {}: {}", msgId, ex.getMessage(), ex);
                    saveInvalidByPassMsgAndSendToKafka(xmlMessage, msgId, topic, msgType, target, batchId);
                    return Mono.just(AuditStatus.ERROR);
                });
    }


    private void saveInvalidByPassMsgAndSendToKafka(String[] xml, String msgId, String topic, String msgType, String target, String batchId) {
        String messageId = "";

        if (msgId == null || msgId.isBlank()) {
            // Try extracting from concatenated XML parts
            messageId = sfmsConsmrCommonUtility.getMsgId(xml[0] + xml[1]);
            msgId = messageId;
            if (messageId == null || messageId.isBlank()) {
                log.warn("MsgId is Null");
            }
        }
        if (target==null)
            target=bypassProperties.getDefaultSwitch();

        if(msgType==null)
            msgType= sfmsConsmrCommonUtility.getMsgType(xml[0]+xml[1]);

        kafkaUtils.publishToKafkaTopic(xml[0] + xml[1], topic, msgId);


        String finalMsgId = msgId;
        sfmsConsumerRepository.saveDataInInvalidPayload(msgId, msgType, xml[0] + xml[1], target, true)
                .subscribe(status -> log.info("Bypass record saved for msgId {}: {}", finalMsgId, status),
                        error -> log.error("Failed to save bypass payload for msgId {}: {}", finalMsgId, error.getMessage(), error));
    }
}
