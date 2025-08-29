package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.TargetProcessorTopicConfig;
import com.hdfcbank.sfmsconsumer.dao.NilRepository;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.BatchTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class IncomingMsgAuditTest {
    @Mock
    private NilRepository nilRepository;

    @Mock
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Mock
    private TargetProcessorTopicConfig config;

    @Mock
    private BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @InjectMocks
    private IncomingMsgAudit incomingMsgAudit;

    private String[] xmlMessage;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Minimal valid XML
        String xml = "<RequestPayload>" +
                "<AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr>" +
                "<Document><GrpHdr><MsgId>RBIP123</MsgId></GrpHdr></Document>" +
                "</RequestPayload>";

        xmlMessage = new String[]{"", xml};

        // Default stubbing for all xpath calls
        lenient().when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), anyString()))
                .thenAnswer(invocation -> {
                    String xpath = invocation.getArgument(1);
                    if (xpath.contains("MsgId")) return "RBIP123";
                    if (xpath.contains("MsgDefIdr")) return "pacs.008.001.09";
                    return null;
                });

        lenient().when(config.getProcessorFileType(anyString())).thenReturn("TARGET_TOPIC");
        lenient().when(batchIdXmlFieldExtractor.extractFieldByFileType(anyString())).thenReturn("BATCH123");
    }

    @Test
    void testAuditIncomingMessage_nonAdmiMessage_savesMsgEventAndBatch() {
        // Act
        incomingMsgAudit.auditIncomingMessage(xmlMessage);

        // Assert
        verify(nilRepository, times(1)).saveDataInMsgEventTracker(any(MsgEventTracker.class));
        verify(nilRepository, times(1)).saveMsgInBatchTracker(any(BatchTracker.class));
        verify(nilRepository, never()).saveDataInAdmiTracker(any(AdmiTracker.class));
    }

    @Test
    void testAuditIncomingMessage_admiMessage_savesAdmiAndBatch() throws XPathExpressionException {
        // Arrange - override msgType to ADMI
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class), contains("MsgDefIdr")))
                .thenReturn("admi.004.001.02");

        // Act
        incomingMsgAudit.auditIncomingMessage(xmlMessage);

        // Assert
        verify(nilRepository, times(1)).saveDataInAdmiTracker(any(AdmiTracker.class));
        verify(nilRepository, times(1)).saveMsgInBatchTracker(any(BatchTracker.class));
        verify(nilRepository, never()).saveDataInMsgEventTracker(any(MsgEventTracker.class));
    }

    @Test
    void testAuditIncomingMessage_batchIdNull_savesEmptyBatchId() throws Exception {
        // Arrange
        when(batchIdXmlFieldExtractor.extractFieldByFileType(anyString())).thenReturn(null);

        // Act
        incomingMsgAudit.auditIncomingMessage(xmlMessage);

        // Assert
        ArgumentCaptor<BatchTracker> batchCaptor = ArgumentCaptor.forClass(BatchTracker.class);
        verify(nilRepository).saveMsgInBatchTracker(batchCaptor.capture());
        BatchTracker savedBatch = batchCaptor.getValue();

        // BatchId should be empty string if extractor returned null
        org.junit.jupiter.api.Assertions.assertEquals("", savedBatch.getBatchId());
    }

    @Test
    void testAuditIncomingMessage_exceptionThrown_wrapsInRuntimeException() throws Exception {
        // Arrange - make extractor throw exception
        when(batchIdXmlFieldExtractor.extractFieldByFileType(anyString()))
                .thenThrow(new RuntimeException("Extractor error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> incomingMsgAudit.auditIncomingMessage(xmlMessage));
    }
}