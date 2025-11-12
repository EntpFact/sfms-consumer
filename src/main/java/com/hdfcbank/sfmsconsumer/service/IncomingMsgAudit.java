package com.hdfcbank.sfmsconsumer.service;


import com.hdfcbank.sfmsconsumer.config.InvalidAndExceptionMsgTopic;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.AuditStatus;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@AllArgsConstructor
public class IncomingMsgAudit {

    @Autowired
    private final SFMSConsumerRepository sfmsConsumerRepository;

    @Autowired
    private final SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    private final TargetProcessorTopicConfig config;

    @Autowired
    private final BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @Autowired
    private final BuildJsonReq buildJsonReq;

    @Autowired
    KafkaUtils kafkaUtils;

    @Autowired
    private InvalidAndExceptionMsgTopic topic;


    public Mono<AuditStatus> auditIncomingMessage(String[] xmlMessage) {
        return Mono.defer(() -> {
            String jsonReq = null;
            String msgId = null;
            String msgType = null;
            String target = null;
            String batchId = null;
            String batchDateTime = null;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(xmlMessage[1])));

                msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
                msgType = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
                target = config.getProcessorFileType(msgType);
                batchId = batchIdXmlFieldExtractor.extractFieldByFileType(xmlMessage[1]);
                batchDateTime = sfmsConsmrCommonUtility.getValueByXPath(document, BATCH_CREDT_XPATH);

                Instant instant = Instant.parse(batchDateTime);
                LocalDateTime localDateTime = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
                jsonReq = buildJsonReq.buildRequest(xmlMessage);

                return msgType.contains(ADMI)
                        ? handleAdmiTracker(xmlMessage, msgId, msgType, target, localDateTime)
                        : handleMsgEventTracker(xmlMessage, msgId, msgType, target, batchId, localDateTime);

            } catch (Exception e) {
                log.error("Error during message audit: {}", e.getMessage(), e);
                log.error("Routing failed message to exception topic due to error: {}", e.getMessage());
                String errjson = buildJsonReq.handleExceptionRouting(xmlMessage, e, msgId, msgType, batchId, batchDateTime, DATABASE_ERROR);
                kafkaUtils.publishToResponseTopic(errjson, topic.getExceptionTopic(), msgId,batchId);
                return Mono.just(AuditStatus.ERROR);
            }
        });
    }

    private Mono<AuditStatus> handleAdmiTracker(String[] xmlMessage, String msgId, String msgType,
                                                String target, LocalDateTime batchDateTime) {

        AdmiTracker admiTracker = AdmiTracker.builder()
                .msgId(msgId)
                .msgType(msgType)
                .target(target)
                .invalidReq(false)
                .orgnlReq(xmlMessage[0] + xmlMessage[1])
                .batchCreationTimestamp(batchDateTime)
                .build();

        return sfmsConsumerRepository.saveDataInAdmiTracker(admiTracker)
                .flatMap(status -> {
                    // Handle specific duplicate cases In AdmiTracker
                    if (status == AuditStatus.CAPTURED_DUPLICATE) {
                        log.info("CAPTURED_DUPLICATE detected for msgId: {} — treating as SUCCESS", msgId);
                        return Mono.just(AuditStatus.SUCCESS);
                    } else if (status == AuditStatus.SEND_TO_PROCESSOR_DUPLICATE) {
                        log.info("SEND_TO_PROCESSOR_DUPLICATE detected for msgId: {} — treating as DUPLICATE", msgId);
                        return Mono.just(AuditStatus.DUPLICATE);
                    } else if (status == AuditStatus.SEND_TO_DISPATCHER) {
                        log.info("SEND_TO_DISPATCHER detected for msgId: {} — treating as DUPLICATE", msgId);
                        return Mono.just(AuditStatus.DUPLICATE);
                    } else if (status == AuditStatus.DUPLICATE) {
                        log.warn("Generic DUPLICATE detected for msgId: {} — incrementing version", msgId);
                        return sfmsConsumerRepository.incrementAdmiTrackerVersion(msgId)
                                .thenReturn(AuditStatus.DUPLICATE);
                    }

                    // For all other statuses
                    return Mono.just(status);
                })
                .onErrorResume(ex -> {

                    String errMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

                    boolean isDbDown =
                            ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException ||
                                    ex instanceof io.r2dbc.spi.R2dbcTransientResourceException ||
                                    errMsg.toLowerCase().contains("connection refused") ||
                                    errMsg.toLowerCase().contains("timeout") ||
                                    errMsg.toLowerCase().contains("connection reset") ||
                                    errMsg.toLowerCase().contains("database is unavailable");

                    if (isDbDown) {
                        // Database down → publish original XML to negative ACK topic
                        log.error("Database DOWN detected for msgId {}: {}", msgId, errMsg);
                        kafkaUtils.publishToResponseTopic(xmlMessage[0] + xmlMessage[1], target, msgId, null);
                        return Mono.just(AuditStatus.ERROR);
                    }

                    // Other database or runtime exceptions
                    log.error("Error while saving AdmiTracker for msgId {}: {}", msgId, ex.getMessage(), ex);
                    log.error("Routing failed message to exception topic due to error");

                    String errJson = buildJsonReq.handleExceptionRouting(
                            xmlMessage, ex, msgId, msgType, null,
                            batchDateTime != null ? batchDateTime.toString() : "", DATABASE_ERROR);

                    kafkaUtils.publishToResponseTopic(errJson, topic.getExceptionTopic(), msgId, null);
                    return Mono.just(AuditStatus.ERROR);
                });
    }


    private Mono<AuditStatus> handleMsgEventTracker(String[] xmlMessage, String msgId, String msgType,
                                                    String target, String batchId, LocalDateTime batchDateTime) {

        MsgEventTracker msgEventTracker = MsgEventTracker.builder()
                .msgId(msgId)
                .msgType(msgType)
                .source(SFMS)
                .flowType(INWARD)
                .invalidReq(false)
                .target(target)
                .batchId(batchId != null ? batchId : "")
                .orgnlReq(xmlMessage[0] + xmlMessage[1])
                .batchCreationTimestamp(batchDateTime)
                .build();

        return sfmsConsumerRepository.saveDataInMsgEventTracker(msgEventTracker)
                .flatMap(status -> {

                    // New handling logic for specific duplicate types
                    if (status == AuditStatus.CAPTURED_DUPLICATE) {
                        log.info("CAPTURED_DUPLICATE detected for msgId: {} — treating as SUCCESS", msgId);
                        return Mono.just(AuditStatus.SUCCESS);
                    } else if (status == AuditStatus.SEND_TO_PROCESSOR_DUPLICATE) {
                        log.info("SEND_TO_PROCESSOR_DUPLICATE detected for msgId: {} — treating as DUPLICATE", msgId);
                        return Mono.just(AuditStatus.DUPLICATE);
                    } else if (status == AuditStatus.SEND_TO_DISPATCHER) {
                        log.info("SEND_TO_DISPATCHER detected for msgId: {} — treating as DUPLICATE", msgId);
                        return Mono.just(AuditStatus.DUPLICATE);
                    }

                    return Mono.just(status);
                })
                .onErrorResume(ex -> {

                    String errMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

                    boolean isDbDown =
                            ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException ||
                                    ex instanceof io.r2dbc.spi.R2dbcTransientResourceException ||
                                    errMsg.toLowerCase().contains("connection refused") ||
                                    errMsg.toLowerCase().contains("timeout") ||
                                    errMsg.toLowerCase().contains("connection reset") ||
                                    errMsg.toLowerCase().contains("database is unavailable");

                    if (isDbDown) {
                        // Database down → send original XML to negative ACK topic
                        log.error("Database DOWN detected for msgId {}: {}", msgId, errMsg);
                        kafkaUtils.publishToResponseTopic(xmlMessage[0] + xmlMessage[1], target, msgId,batchId != null ? batchId : "");
                        return Mono.just(AuditStatus.ERROR);
                    }

                    log.error("Error while saving MsgEventTracker for msgId {}: {}", msgId, ex.getMessage(), ex);
                    log.error("Routing failed message to exception topic due to error");
                    String errJson = buildJsonReq.handleExceptionRouting(
                            xmlMessage, ex, msgId, msgType, batchId, batchDateTime.toString(), DATABASE_ERROR);
                    kafkaUtils.publishToResponseTopic(errJson, topic.getExceptionTopic(), msgId,batchId != null ? batchId : "");

                    return Mono.just(AuditStatus.ERROR);
                });
    }


}