package com.hdfcbank.sfmsconsumer.service;


import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.AuditStatus;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
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
    SFMSConsumerRepository sfmsConsumerRepository;

    @Autowired
    SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    private final TargetProcessorTopicConfig config;

    @Autowired
    BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @Autowired
    private ErrXmlRoutingService errorMsgAudit;

    @Autowired
    BuildJsonReq buildJsonReq;

    //@Transactional
    public Mono<AuditStatus> auditIncomingMessage(String[] xmlMessage) {
        return Mono.defer(() -> {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(xmlMessage[1])));

                String msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
                String msgType = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
                String target = config.getProcessorFileType(msgType);
                String batchId = batchIdXmlFieldExtractor.extractFieldByFileType(xmlMessage[1]);
                String batchDateTime = sfmsConsmrCommonUtility.getValueByXPath(document, BATCH_CREDT_XPATH);
                Instant instant = Instant.parse(batchDateTime);
                LocalDateTime localDateTime = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
                String jsonReq = buildJsonReq.buildRequest(xmlMessage);

                // Call appropriate tracker handler
                return msgType.contains(ADMI)
                        ? handleAdmiTracker(xmlMessage, msgId, msgType, target, jsonReq, localDateTime)
                        : handleMsgEventTracker(xmlMessage, msgId, msgType, target, batchId, jsonReq, localDateTime);

                //return trackerMono;

            } catch (Exception e) {
                log.error("Error during message audit: {}", e.getMessage(), e);
                errorMsgAudit.determineTopic(xmlMessage[0] + xmlMessage[1]);
                return Mono.just(AuditStatus.ERROR);
            }
        });
    }

    private Mono<AuditStatus> handleAdmiTracker(String[] xmlMessage, String msgId, String msgType, String target, String jsonReq,LocalDateTime batchDateTime) {
        //try {
            AdmiTracker admiTracker = AdmiTracker.builder()
                    .msgId(msgId)
                    .msgType(msgType)
                    .target(target)
                    .invalidReq(false)
                    //.transformedJsonReq(jsonReq)
                    .orgnlReq(xmlMessage[0] + xmlMessage[1])
                    .batchCreationTimestamp(batchDateTime)
                    .build();

            return sfmsConsumerRepository.saveDataInAdmiTracker(admiTracker)
                    .flatMap(status -> {
                        if (status == AuditStatus.DUPLICATE) {
                            log.warn("Duplicate entry for msgId: {} — updating version", msgId);
                            return sfmsConsumerRepository.incrementAdmiTrackerVersion(msgId)
                                    .thenReturn(AuditStatus.DUPLICATE);
                        }
                        return Mono.just(status); // SUCCESS or ERROR
                    })
                    .onErrorResume(ex -> {
                        log.error("Error while saving AdmiTracker for msgId {}: {}", msgId, ex.getMessage(), ex);
                        return Mono.just(AuditStatus.ERROR);
                    });
    }

    private Mono<AuditStatus> handleMsgEventTracker(String[] xmlMessage, String msgId, String msgType,
                                                    String target, String batchId, String jsonReq, LocalDateTime batchDateTime) {
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
                    if (status == AuditStatus.DUPLICATE) {
                        log.warn("Duplicate entry for msgId: {} — updating version", msgId);
                        return sfmsConsumerRepository.incrementMsgEventTrackerVersion(msgId)
                                .thenReturn(AuditStatus.DUPLICATE);
                    }
                    return Mono.just(status); // SUCCESS or ERROR
                })
                .onErrorResume(ex -> {
                    log.error("Error while saving MsgEventTracker for msgId {}: {}", msgId, ex.getMessage(), ex);
                    return Mono.just(AuditStatus.ERROR);
                });

    }


}