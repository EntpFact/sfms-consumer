package com.hdfcbank.sfmsconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.config.InvalidAndExceptionMsgTopic;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.exception.SFMSException;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.Body;
import com.hdfcbank.sfmsconsumer.model.ExceptionDetail;
import com.hdfcbank.sfmsconsumer.model.Header;
import com.hdfcbank.sfmsconsumer.model.ReqPayload;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import static com.hdfcbank.sfmsconsumer.utils.Constants.*;
import static com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility.toXmlDocument;

@Slf4j
@Service
@AllArgsConstructor
public class BuildJsonReq {

    @Autowired
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Autowired
    KafkaUtils kafkaUtils;

    @Autowired
    BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    private final TargetProcessorTopicConfig config;

    private final InvalidAndExceptionMsgTopic topicDetails;

    public String buildRequest(String xml[]) {
        String xmlMessage = xml[0] + xml[1];
        String json = null;
        String msgId = null;
        String msgDefIdr = null;
        String target = null;
        String batchId = null;
        String batchDateTime = null;
        try {
            Document document = toXmlDocument(xml[1]);
            msgDefIdr = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
            msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
            target = config.getProcessorFileType(msgDefIdr.trim());
            batchId = batchIdXmlFieldExtractor.extractFieldByFileType(xml[1]);
            batchDateTime = sfmsConsmrCommonUtility.getValueByXPath(document, BATCH_CREDT_XPATH);

            log.info("msgDefIdr : {}", msgDefIdr);
            // log.info("Xml Message : " + xmlMessage);


            Header header = Header.builder()
                    .msgId(msgId)
                    .msgType(msgDefIdr)
                    .source(SFMS)
                    .target(target)
                    .flowType(INWARD)
                    .replayInd(false)
                    .replayCount(0)
                    .prefix(xml[0])
                    .status(CAPTURED)
                    .batchCreDt(batchDateTime)
                    .batchId(batchId)
                    .build();

            Body body = Body.builder()
                    .payload(xml[1])
                    .build();

            ExceptionDetail exceptionDetail = ExceptionDetail.builder()
                    .exceptionType(null)
                    .exceptionDesc(null)
                    .build();

            ReqPayload reqPayload = ReqPayload.builder()
                    .header(header)
                    .body(body)
                    .exceptionDetail(exceptionDetail)
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(reqPayload);
            log.info("Json : {}", json);

        } catch (Exception e) {
            log.error(e.toString());
            log.error("Error generating JSON ");
            log.error("Routing failed message to exception topic due to error: {}", e.getMessage());
            String errjson = handleExceptionRouting(xml, e, msgId, msgDefIdr, batchId, batchDateTime, JSON_PROCESSING_ERROR);
            kafkaUtils.publishToKafkaTopic(errjson, topicDetails.getExceptionTopic(), msgId);
        }

        return json;
    }

    /**
     * Handles routing failed messages to Kafka exception topic with code.
     *
     * @return
     */
    public String handleExceptionRouting(String[] xmlMessage, Throwable ex, String msgId, String msgType, String batchId, String batchDateTime, String exceptionType) {
        String json = null;
        ReqPayload reqPayload = new ReqPayload();
        try {

            Header header = Header.builder()
                    .msgId(msgId)
                    .msgType(msgType)
                    .source(SFMS)
                    .target(SFMS_CONSUMER)
                    .flowType(INWARD)
                    .replayInd(false)
                    .replayCount(0)
                    .prefix(xmlMessage[0])
                    .status(CAPTURED)
                    .batchCreDt(batchDateTime)
                    .batchId(batchId)
                    .build();

            Body body = Body.builder()
                    .payload(xmlMessage[1])
                    .build();

            ExceptionDetail exceptionDetail = ExceptionDetail.builder()
                    .exceptionType(exceptionType)
                    .exceptionDesc(ex.getMessage())
                    .build();

            reqPayload.setHeader(header);
            reqPayload.setBody(body);
            reqPayload.setExceptionDetail(exceptionDetail);

            //Send it to Exception Topic
            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqPayload);

        } catch (Exception e) {
            log.error("Json Processing Error: {}", e.getMessage(), e);
            throw new SFMSException("JSON_PROCESSING_ERROR: " + reqPayload, e);
        }
        return json;
    }
}
