package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.BatchIdXPathConfig;
import com.hdfcbank.sfmsconsumer.utils.SfmsConsmrCommonUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGDEFIDR_XPATH;
import static com.hdfcbank.sfmsconsumer.utils.Constants.MSGID_XPATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchIdXmlFieldExtractorTest {


    @Mock
    private SfmsConsmrCommonUtility sfmsConsmrCommonUtility;

    @Mock
    private BatchIdXPathConfig batchIdXPathMap;

    @InjectMocks
    private BatchIdXmlFieldExtractor extractor;

    private String validXml;

    @BeforeEach
    void setUp() {
        validXml = """
                <RequestPayload xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09">
                    <AppHdr>
                        <MsgDefIdr>pacs.008.001.09</MsgDefIdr>
                        <BizMsgIdr>RBIP202507236244045029</BizMsgIdr>
                    </AppHdr>
                    <Document>
                        <FIToFICstmrCdtTrf>
                            <GrpHdr>
                                <MsgId>BATCH12345</MsgId>
                            </GrpHdr>
                        </FIToFICstmrCdtTrf>
                    </Document>
                </RequestPayload>
                """;

        // extractor = new BatchIdXmlFieldExtractor(sfmsConsmrCommonUtility, batchIdXPathMap);
    }

    @Test
    void testExtractFieldByFileType_success() throws Exception {
        String xml = "<RequestPayload>"
                + "<AppHdr>"
                + "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>"
                + "<BizMsgIdr>RBIP202507236244045029</BizMsgIdr>"
                + "</AppHdr>"
                + "<Document><SomeTag>Value</SomeTag></Document>"
                + "</RequestPayload>";

        // stub for MsgDefIdr
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class),
                eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']")))
                .thenReturn("pacs.008.001.09");

        // stub for BatchId extraction xpath
        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='SomeTag']");

        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class),
                eq("//*[local-name()='SomeTag']")))
                .thenReturn("BATCH123");

        String result = extractor.extractFieldByFileType(xml);

        assertEquals("BATCH123", result);
    }

    @Test
    void testExtractFieldByFileType_msgDefIdrMissing_returnsNull() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class),
                eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']")))
                .thenReturn(null);

        // Act
        String result = extractor.extractFieldByFileType(validXml);

        // Assert
        assertNull(result);
        verify(sfmsConsmrCommonUtility, times(1))
                .getValueByXPath(any(Document.class), eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']"));
        verifyNoInteractions(batchIdXPathMap);
    }

/*    @Test
    void testExtractFieldByFileType_xpathMissing_returnsNull() throws Exception {
        // Arrange
        when(sfmsConsmrCommonUtility.getValueByXPath(any(Document.class),
                eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']")))
                .thenReturn("pacs.008.001.09");

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn(null);

        // Act
        String result = extractor.extractFieldByFileType(validXml);

        // Assert
        assertNull(result);
        verify(sfmsConsmrCommonUtility, times(1))
                .getValueByXPath(any(Document.class), eq("//*[local-name()='AppHdr']/*[local-name()='MsgDefIdr']"));
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }*/

} 