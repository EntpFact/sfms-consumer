package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishMessageTest {

    @Mock
    private TargetProcessorTopicConfig config;

    @Mock
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Mock
    private KafkaUtils kafkaUtils;

    @Mock
    private BuildJsonReq buildJsonReq;

    @InjectMocks
    private PublishMessage publishMessage;

    private String[] xml;

    @BeforeEach
    void setUp() {
        xml = new String[]{
                "CBS", // prefix
                "<RequestPayload>" +
                        "<AppHdr><BizMsgIdr>RBIP20250911003</BizMsgIdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr>" +
                        "<Document><MsgDefIdr>pacs.008.001.09</MsgDefIdr></Document>" +
                        "</RequestPayload>"
        };
    }
    @Test
    void testSendRequest_success() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(
                any(Document.class),
                eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']"))
        ).thenReturn("pacs.008.001.09");

        when(buildJsonReq.buildRequest(xml)).thenReturn("{\"json\":\"value\"}");

        // both methods must be stubbed
        when(config.getProcessorFileType("pacs.008.001.09")).thenReturn("pacs008Processor");
        when(config.getTopicFileType("pacs008Processor")).thenReturn("topic-pacs008");

        // Act
        publishMessage.sendRequest(xml);

        // Assert
        verify(kafkaUtils).publishToResponseTopic("{\"json\":\"value\"}", "topic-pacs008");
    }

    @Test
    void testSendRequest_msgDefIdrWithSpaces() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenReturn("  pacs.004.001.09   "); // with spaces

        when(buildJsonReq.buildRequest(xml)).thenReturn("{\"json\":\"value2\"}");

        // both processor + topic mappings needed
        when(config.getProcessorFileType("pacs.004.001.09")).thenReturn("pacs004Processor");
        when(config.getTopicFileType("pacs004Processor")).thenReturn("topic-pacs004");

        // Act
        publishMessage.sendRequest(xml);

        // Assert
        verify(kafkaUtils).publishToResponseTopic("{\"json\":\"value2\"}", "topic-pacs004");
    }

    @Test
    void testSendRequest_xpathExceptionWrapped1() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenThrow(new XPathExpressionException("Invalid XPath"));

        // Act + Assert
        assertThrows(RuntimeException.class, () -> publishMessage.sendRequest(xml));

        // Kafka must not be called
        verifyNoInteractions(kafkaUtils);
    }

    @Test
    void testSendRequest_xpathExceptionWrapped() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenThrow(new XPathExpressionException("Invalid XPath"));

        // Act + Assert
        assertThrows(RuntimeException.class, () -> publishMessage.sendRequest(xml));
        verifyNoInteractions(kafkaUtils);
    }
}

