package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.NilRepository;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private NilRepository nilRepository;

    @Mock
    private TargetProcessorTopicConfig config;

    @InjectMocks   // ✅ Mockito will inject mocks into this service
    private ErrXmlRoutingService errXmlRoutingService;

    Map<String, String> topicMap = new HashMap<>();
    Map<String, String> processorMap = new HashMap<>();

    @BeforeEach
    void setUp() {
        // Mock config topics

        topicMap.put("pacs.008", "topic-pacs008");
        topicMap.put("pacs.004", "topic-pacs004");


        processorMap.put("pacs.008", "PROC008");
        processorMap.put("pacs.004", "PROC004");


    }

    @Test
    void testDetermineTopic_pacs008Message_PublishesToKafka() {
        String xmlMessage =
                "<RequestPayload>" +
                        "<BizMsgIdr>MSG123</BizMsgIdr>" +
                        "<Document>pacs.008 data here</Document>" +
                        "</RequestPayload>";
        when(config.getTopic()).thenReturn(topicMap);
        when(config.getProcessor()).thenReturn(processorMap);
        when(config.getProcessorFileType(anyString())).thenReturn("PROC008");

        errXmlRoutingService.determineTopic(xmlMessage);

        // Verify repository save called
        verify(nilRepository, times(1)).saveDataInMsgEventTracker(any(MsgEventTracker.class));

        // Verify Kafka publish called with topic "topic-pacs008"
        verify(kafkaUtils, times(1)).publishToResponseTopic(anyString(), eq("topic-pacs008"));
    }

    @Test
    void testDetermineTopic_pacs004Message_PublishesToKafka() {
        String xmlMessage =
                "<RequestPayload>" +
                        "<BizMsgIdr>MSG999</BizMsgIdr>" +
                        "<Document>pacs.004 something</Document>" +
                        "</RequestPayload>";

        when(config.getTopic()).thenReturn(topicMap);
        when(config.getProcessor()).thenReturn(processorMap);
        when(config.getProcessorFileType(anyString())).thenReturn("PROC008");

        errXmlRoutingService.determineTopic(xmlMessage);


        verify(nilRepository, times(1)).saveDataInMsgEventTracker(any(MsgEventTracker.class));
        verify(kafkaUtils, times(1)).publishToResponseTopic(anyString(), eq("topic-pacs004"));
    }

    @Test
    void testDetermineTopic_NoMatch_DoesNothing() {
        String xmlMessage =
                "<RequestPayload>" +
                        "<BizMsgIdr>MSG000</BizMsgIdr>" +
                        "<Document>unknown</Document>" +
                        "</RequestPayload>";

        errXmlRoutingService.determineTopic(xmlMessage);

        // No publish or save should happen
        verifyNoInteractions(kafkaUtils);
        verifyNoInteractions(nilRepository);
    }

    @Test
    void testErrorMessageAudit_SavesTracker() {
        String xmlMessage = "<RequestPayload><BizMsgIdr>MSG-AUDIT</BizMsgIdr></RequestPayload>";

        errXmlRoutingService.errorMessageAudit(xmlMessage, "pacs.008");

        verify(nilRepository, times(1)).saveDataInMsgEventTracker(any(MsgEventTracker.class));
    }

    @Test
    void testGetMsgId_Found() throws Exception {
        String xmlMessage = "<RequestPayload><BizMsgIdr>MSGFOUND</BizMsgIdr></RequestPayload>";

        String msgId = invokeGetMsgId(xmlMessage);

        assert msgId.equals("MSGFOUND");
    }

    @Test
    void testGetMsgId_NotFound_ReturnsNull() throws Exception {
        String xmlMessage = "<RequestPayload><NoBizMsgIdr>Missing</NoBizMsgIdr></RequestPayload>";

        String msgId = invokeGetMsgId(xmlMessage);

        assert msgId == null;
    }

    // --- helper to invoke private method getMsgId ---
    private String invokeGetMsgId(String xmlMessage) throws Exception {
        var method = ErrXmlRoutingService.class.getDeclaredMethod("getMsgId", String.class);
        method.setAccessible(true);
        return (String) method.invoke(errXmlRoutingService, xmlMessage);
    }
}
