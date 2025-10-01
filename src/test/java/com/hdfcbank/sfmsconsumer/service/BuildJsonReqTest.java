package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildJsonReqTest {

    @Mock
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Mock
    private ErrXmlRoutingService errXmlRoutingService;

    @Mock
    private TargetProcessorTopicConfig config;

    @InjectMocks
    private BuildJsonReq buildJsonReq;

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
    void testBuildRequest_success() throws Exception {
        // Arrange - stubbing with exact XPath constants
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class),
                eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']")))
                .thenReturn("pacs.008.001.09");

        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class),
                eq("//*[local-name()='AppHdr']/*[local-name()='BizMsgIdr']")))
                .thenReturn("RBIP20250911003");

        when(config.getProcessorFileType("pacs.008.001.09")).thenReturn("PROC008");

        // Act
        String json = buildJsonReq.buildRequest(xml);

        // Assert
        assertNotNull(json, "JSON should not be null on success");
        assertTrue(json.contains("\"msgId\":\"RBIP20250911003\""));
        assertTrue(json.contains("\"msgType\":\"pacs.008.001.09\""));
        assertTrue(json.contains("\"target\":\"PROC008\""));
        assertTrue(json.contains("\"prefix\":\"CBS\""));
        assertTrue(json.contains(xml[1]));

        // errXmlRoutingService should NOT be called
        verify(errXmlRoutingService, never()).determineTopic(anyString());
    }

    @Test
    void testBuildRequest_exceptionFlow() throws Exception {
        // Arrange - make utility throw exception
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenThrow(new RuntimeException("XPath error"));

        // Act
        String json = buildJsonReq.buildRequest(xml);

        // Assert
        assertNull(json, "JSON should be null when exception occurs");
        verify(errXmlRoutingService).determineTopic(anyString());
    }
}