package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.xpath.XPathExpressionException;

import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGDEFIDR_XPATH;
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


    public Mono<Void> sendRequest(String[] xml) {
        return Mono.fromRunnable(() -> {
            try {
                Document document = toXmlDocument(xml[1]);
                String msgDefIdr = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
                String jsonReq = buildJsonReq.buildRequest(xml);
                String target = config.getProcessorFileType(msgDefIdr.trim());
                String topic = config.getTopicFileType(target.trim());

                // Send to respective processor topic
                log.info("JSON : {}", jsonReq);
                kafkaUtils.publishToResponseTopic(jsonReq, topic);

            } catch (XPathExpressionException e) {
                log.error("XPath parsing error while publishing: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                log.error("Error in sendRequest: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }


}