package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrXmlRoutingServiceTest {
    @Mock
    private KafkaUtils kafkaUtils;

    @Mock
    private SFMSConsumerRepository sfmsConsumerRepository;

    private ErrXmlRoutingService service;
/*

    @BeforeEach
    void setUp() {
        // Mock config
        TargetProcessorTopicConfig config = mock(TargetProcessorTopicConfig.class);

        // processor mappings
        Map<String, String> processorMap = new HashMap<>();
        processorMap.put("pacs.008", "PROC008");
        processorMap.put("pacs.004", "PROC004");
        processorMap.put("admi.004", "PROC_ADMI");

        // topic mappings
        Map<String, String> topicMap = new HashMap<>();
        topicMap.put("PROC008", "topic-pacs008");
        topicMap.put("PROC004", "topic-pacs004");
        topicMap.put("PROC_ADMI", "topic-admi");

        when(config.getProcessor()).thenReturn(processorMap);
        when(config.getTopicFileType(anyString())).thenAnswer(inv -> topicMap.get(inv.getArgument(0)));
        when(config.getProcessorFileType(anyString())).thenAnswer(inv -> processorMap.get(inv.getArgument(0)));

        service = new ErrXmlRoutingService(kafkaUtils, sfmsConsumerRepository, config);
    }

    @Test
    void testDetermineTopic_Pacs008() {
        String xmlMessage = """
                <RequestPayload>
                  <AppHdr><BizMsgIdr>RBIP20250911001</BizMsgIdr></AppHdr>
                  <Document><MsgDefIdr>pacs.008</MsgDefIdr></Document>
                </RequestPayload>
                """;

        service.determineTopic(xmlMessage);

        verify(kafkaUtils, times(1))
                .publishToResponseTopic(anyString(), eq("topic-pacs008"));

        verify(sfmsConsumerRepository, times(1))
                .saveDataInMsgEventTracker(any(MsgEventTracker.class));
    }

    @Test
    void testDetermineTopic_Pacs004() {
        String xmlMessage = """
                <RequestPayload>
                  <AppHdr><BizMsgIdr>RBIP20250911002</BizMsgIdr></AppHdr>
                  <Document><MsgDefIdr>pacs.004</MsgDefIdr></Document>
                </RequestPayload>
                """;

        service.determineTopic(xmlMessage);

        verify(kafkaUtils, times(1))
                .publishToResponseTopic(anyString(), eq("topic-pacs004"));

        verify(sfmsConsumerRepository, times(1))
                .saveDataInMsgEventTracker(any(MsgEventTracker.class));
    }

    @Test
    void testDetermineTopic_Admi004() {
        String xmlMessage = """
                <RequestPayload>
                  <AppHdr><BizMsgIdr>RBIP20250911003</BizMsgIdr></AppHdr>
                  <Document><MsgDefIdr>admi.004</MsgDefIdr></Document>
                </RequestPayload>
                """;

        service.determineTopic(xmlMessage);

        verify(kafkaUtils, times(1))
                .publishToResponseTopic(anyString(), eq("topic-admi"));

        verify(sfmsConsumerRepository, times(1))
                .saveDataInAdmiTracker(any(AdmiTracker.class));
    }
*/

}
