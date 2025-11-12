package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.InvalidAndExceptionMsgTopic;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.AuditStatus;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.xml.xpath.XPathExpressionException;

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;
import static com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility.toXmlDocument;


@Slf4j
@Service
@AllArgsConstructor
public class PublishMessage {
    private final TargetProcessorTopicConfig config;

    @Autowired
    SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Autowired
    KafkaUtils kafkaUtils;

    @Autowired
    BuildJsonReq buildJsonReq;

    @Autowired
    private final BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    private final InvalidAndExceptionMsgTopic topicDetails;

    @Autowired
    private SFMSConsumerRepository sfmsConsumerRepository;

    public Mono<Void> sendRequest(String[] xml) {
        return Mono.defer(() -> {
            String msgId = null, msgType = null, batchId = null, batchDateTime = null;

            try {
                Document document = toXmlDocument(xml[1]);
                msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
                msgType = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
                batchId = batchIdXmlFieldExtractor.extractFieldByFileType(xml[1]);
                batchDateTime = sfmsConsmrCommonUtility.getValueByXPath(document, BATCH_CREDT_XPATH);
            } catch (Exception e) {
                log.error("XML parsing failed: {}", e.getMessage(), e);
                String errJson = buildJsonReq.handleExceptionRouting(
                        xml, e, msgId, msgType, batchId, batchDateTime, "XPATH_PARSING_ERROR");
                return kafkaUtils.publishToResponseTopic(
                        errJson, topicDetails.getExceptionTopic(), msgId, batchId
                ).then();
            }

            String jsonReq = buildJsonReq.buildRequest(xml);
            String target = config.getProcessorFileType(msgType.trim());
            String topic = config.getTopicFileType(target.trim());

            final String finalMsgId = msgId;
            final String finalMsgType = msgType;
            final String finalBatchId = batchId;
            final String finalBatchDateTime = batchDateTime;

            return kafkaUtils.publishToResponseTopic(jsonReq, topic, msgId, batchId)
                    .then(
                            // Chain repository update
                            sfmsConsumerRepository.updateStatusToSendToProcessorDynamic(msgId, batchId, msgType)
                                    .doOnNext(status ->
                                            log.info("Updated SEND_TO_PROCESSOR for msgId={} batchId={} status={}",
                                                    finalMsgId, finalBatchId, status))
                                    // Optionally, treat failure as non-blocking but loggable
                                    .onErrorResume(ex -> {
                                        log.error("DB update failed for msgId={} batchId={}: {}",
                                                finalMsgId, finalBatchId, ex.getMessage(), ex);
                                        return Mono.just(AuditStatus.ERROR);
                                    })
                    )
                    // ignore result (AuditStatus) — you only need completion signal
                    .then()
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(ex -> {
                        log.error("Error in sendRequest: {}", ex.getMessage(), ex);
                        String errJson = buildJsonReq.handleExceptionRouting(
                                xml, ex, finalMsgId, finalMsgType, finalBatchId, finalBatchDateTime, "PROCESSING_ERROR");
                        return kafkaUtils.publishToResponseTopic(
                                errJson, topicDetails.getExceptionTopic(), finalMsgId, finalBatchId
                        ).then();
                    });
        });
    }



}