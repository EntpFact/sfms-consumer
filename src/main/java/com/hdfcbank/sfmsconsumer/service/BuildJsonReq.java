package com.hdfcbank.sfmsconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.model.Body;
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
    ErrXmlRoutingService errXmlRoutingService;

    private final TargetProcessorTopicConfig config;

    public String buildRequest(String xml[]) {
        String xmlMessage = xml[0] + xml[1];
        String json = null;
        try {
            Document document = toXmlDocument(xml[1]);
            String msgDefIdr = sfmsConsmrCommonUtility.getValueByXPath(document, MSGDEFIDR_XPATH);
            String msgId = sfmsConsmrCommonUtility.getValueByXPath(document, MSGID_XPATH);
            String target = config.getProcessorFileType(msgDefIdr.trim());

            log.info("msgDefIdr : {}", msgDefIdr);
            log.info("Xml Message : " + xmlMessage);


            Header header = Header.builder()
                    .msgId(msgId)
                    .msgType(msgDefIdr)
                    .source(SFMS)
                    .target(target)
                    .flowType(INWARD)
                    .replayInd(false)
                    .prefix(xml[0])
                    .build();

            Body body = Body.builder()
                    .payload(xml[1])
                    .build();

            ReqPayload reqPayload = ReqPayload.builder()
                    .header(header)
                    .body(body)
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(reqPayload);

        } catch (Exception e) {
            log.error(e.toString());
            errXmlRoutingService.determineTopic(xmlMessage);
        }

        return json;
    }
}
