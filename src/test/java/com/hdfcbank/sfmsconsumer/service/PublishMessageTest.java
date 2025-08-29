package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;

import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGDEFIDR_XPATH;
import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGID_XPATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PublishMessageTest {

    @Mock
    private TargetProcessorTopicConfig config;

    @Mock
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Mock
    private KafkaUtils kafkaUtils;

    @Mock
    private ErrXmlRoutingService errXmlRoutingService;

    @InjectMocks
    private PublishMessage publishMessage;  // class under test

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendRequest_successfulFlow() throws Exception {
        // Arrange
        String prefix = "CBS";
        String payload = "<RequestPayload><AppHdr><MsgDefIdr>pacs.008.001.07</MsgDefIdr>"
                + "<MsgId>RBIP123456789</MsgId></AppHdr></RequestPayload>";
        String[] xml = {prefix, payload};

        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq(MSGDEFIDR_XPATH)))
                .thenReturn("pacs.008.001.07");
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq(MSGID_XPATH)))
                .thenReturn("RBIP123456789");

        when(config.getProcessorFileType("pacs.008.001.07")).thenReturn("ProcessorX");
        when(config.getTopicFileType("pacs.008.001.07")).thenReturn("topic.test");

        // Act
        publishMessage.sendRequest(xml);

        // Assert → verify kafkaUtils was called
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaUtils, times(1)).publishToResponseTopic(jsonCaptor.capture(), topicCaptor.capture());

        String jsonSent = jsonCaptor.getValue();
        assertTrue(jsonSent.contains("RBIP123456789")); // msgId present
        assertTrue(jsonSent.contains("pacs.008.001.07")); // msgDefIdr present
        assertEquals("topic.test", topicCaptor.getValue());

        verify(errXmlRoutingService, never()).determineTopic(anyString());
    }

    @Test
    void testSendRequest_xpathThrowsException_fallbackToErrorRouting() throws Exception {
        // Arrange
        String[] xml = {"CBS", "<InvalidXml>"};

        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenThrow(new XPathExpressionException("Invalid XPath"));

        // Act
        publishMessage.sendRequest(xml);

        // Assert → should fallback to error routing
        verify(kafkaUtils, never()).publishToResponseTopic(anyString(), anyString());
        verify(errXmlRoutingService, times(1)).determineTopic("CBS<InvalidXml>");
    }

    @Test
    void testSendRequest_configReturnsNullTarget_stillPublishes() throws Exception {
        // Arrange
        String[] xml = {"CBS", "<RequestPayload><AppHdr><MsgDefIdr>camt.054.001.08</MsgDefIdr>"
                + "<MsgId>RBIP2222</MsgId></AppHdr></RequestPayload>"};

        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq(MSGDEFIDR_XPATH)))
                .thenReturn("camt.054.001.08");
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq(MSGID_XPATH)))
                .thenReturn("RBIP2222");

        when(config.getProcessorFileType("camt.054.001.08")).thenReturn(null);
        when(config.getTopicFileType("camt.054.001.08")).thenReturn("topic.camt54");

        // Act
        publishMessage.sendRequest(xml);

        // Assert
        verify(kafkaUtils, times(1)).publishToResponseTopic(anyString(), eq("topic.camt54"));
    }

    @Test
    void testSendRequest_invalidXml_fallbackToErrorRouting() {
        // Arrange
        String[] xml = {"CBS", "<RequestPayload><UnclosedTag>"};

        // Act
        publishMessage.sendRequest(xml);

        // Assert
        verify(errXmlRoutingService, times(1)).determineTopic("CBS<RequestPayload><UnclosedTag>");
        verify(kafkaUtils, never()).publishToResponseTopic(anyString(), anyString());
    }
}
