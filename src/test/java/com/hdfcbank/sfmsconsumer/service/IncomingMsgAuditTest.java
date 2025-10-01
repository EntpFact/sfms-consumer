package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.SFMSConsumerRepository;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomingMsgAuditTest {
    @Mock
    private SFMSConsumerRepository sfmsConsumerRepository;

    @Mock
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Mock
    private TargetProcessorTopicConfig config;

    @Mock
    private BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @Mock
    private BuildJsonReq buildJsonReq;

    @InjectMocks
    private IncomingMsgAudit incomingMsgAudit;

    private String[] xml;

    @BeforeEach
    void setUp() {
        xml = new String[]{
                "CBS", // prefix
                "<RequestPayload>" +
                        "<AppHdr><BizMsgIdr>RBIP20250911003</BizMsgIdr></AppHdr>" +
                        "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                        "<Document><MsgDefIdr>pacs.008.001.09</MsgDefIdr></Document>" +
                        "</RequestPayload>"
        };
    }

    @Test
    void testAuditIncomingMessage_admiFlow() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq("//*[local-name()='AppHdr']/*[local-name()='BizMsgIdr']")))
                .thenReturn("RBIP20250911003");
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']")))
                .thenReturn("admi.002.001.01"); // <-- ADMI message
        when(config.getProcessorFileType("admi.002.001.01")).thenReturn("PROC_ADMI");
        when(batchIdXmlFieldExtractor.extractFieldByFileType(xml[1])).thenReturn("BATCH123");
        when(buildJsonReq.buildRequest(xml)).thenReturn("{\"json\":\"value\"}");

        // Act
        incomingMsgAudit.auditIncomingMessage(xml);

        // Assert
        ArgumentCaptor<AdmiTracker> captor = ArgumentCaptor.forClass(AdmiTracker.class);
        verify(sfmsConsumerRepository).saveDataInAdmiTracker(captor.capture());
        AdmiTracker saved = captor.getValue();

        assertEquals("RBIP20250911003", saved.getMsgId());
        assertEquals("admi.002.001.01", saved.getMsgType());
        assertEquals("PROC_ADMI", saved.getTarget());
        assertEquals("{\"json\":\"value\"}", saved.getTransformedJsonReq());
        assertTrue(saved.getOrgnlReq().contains("RequestPayload"));
    }

    @Test
    void testAuditIncomingMessage_normalFlow() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq("//*[local-name()='AppHdr']/*[local-name()='BizMsgIdr']")))
                .thenReturn("RBIP20250911003");
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']")))
                .thenReturn("pacs.008.001.09");
        when(config.getProcessorFileType("pacs.008.001.09")).thenReturn("PROC008");
        when(batchIdXmlFieldExtractor.extractFieldByFileType(xml[1])).thenReturn("BATCH123");
        when(buildJsonReq.buildRequest(xml)).thenReturn("{\"json\":\"value\"}");

        // Act
        incomingMsgAudit.auditIncomingMessage(xml);

        // Assert
        ArgumentCaptor<MsgEventTracker> captor = ArgumentCaptor.forClass(MsgEventTracker.class);
        verify(sfmsConsumerRepository).saveDataInMsgEventTracker(captor.capture());
        MsgEventTracker saved = captor.getValue();

        assertEquals("RBIP20250911003", saved.getMsgId());
        assertEquals("pacs.008.001.09", saved.getMsgType());
        assertEquals("PROC008", saved.getTarget());
        assertEquals("BATCH123", saved.getBatchId());
        assertEquals("{\"json\":\"value\"}", saved.getTransformedJsonReq());
    }

    @Test
    void testAuditIncomingMessage_batchIdNull() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenReturn("RBIP20250911003")
                .thenReturn("pacs.008.001.09");
        when(config.getProcessorFileType("pacs.008.001.09")).thenReturn("PROC008");
        when(batchIdXmlFieldExtractor.extractFieldByFileType(xml[1])).thenReturn(null); // null case
        when(buildJsonReq.buildRequest(xml)).thenReturn("{\"json\":\"value\"}");

        // Act
        incomingMsgAudit.auditIncomingMessage(xml);

        // Assert
        ArgumentCaptor<MsgEventTracker> captor = ArgumentCaptor.forClass(MsgEventTracker.class);
        verify(sfmsConsumerRepository).saveDataInMsgEventTracker(captor.capture());
        MsgEventTracker saved = captor.getValue();

        assertEquals("", saved.getBatchId(), "BatchId should default to empty string when null");
    }

/*    @Test
    void testAuditIncomingMessage_parserErrorHandled() throws Exception {
        // Arrange
        String[] badXml = {"CBS", "<Invalid<xml"}; // malformed
        // No need to stub, parser will fail

        // Act
        assertDoesNotThrow(() -> incomingMsgAudit.auditIncomingMessage(badXml));

        // Assert
        verifyNoInteractions(sfmsConsumerRepository);
    }*/
}