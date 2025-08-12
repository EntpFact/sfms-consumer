package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.config.BatchIdXPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchIdXmlFieldExtractor Tests")
class BatchIdXmlFieldExtractorTest {

    @Mock
    private BatchIdXPathConfig batchIdXPathMap;

    @InjectMocks
    private BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    private String validXmlWithMsgDefIdr;
    private String validXmlWithoutMsgDefIdr;

    @BeforeEach
    void setUp() {
        validXmlWithMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        validXmlWithoutMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";
    }

    @Test
    @DisplayName("Should extract batch ID successfully when MsgDefIdr is found and XPath mapping exists")
    void shouldExtractBatchIdSuccessfully() throws Exception {
        // Given
        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(validXmlWithMsgDefIdr);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should return null when MsgDefIdr is not found in XML")
    void shouldReturnNullWhenMsgDefIdrNotFound() throws Exception {
        // Given
        // No setup needed as MsgDefIdr is not present in validXmlWithoutMsgDefIdr

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(validXmlWithoutMsgDefIdr);

        // Then
        assertNull(result);
        verify(batchIdXPathMap, never()).getXPathForFileType(anyString());
    }

    @Test
    @DisplayName("Should return null when XPath mapping is not found for MsgDefIdr")
    void shouldReturnNullWhenXPathMappingNotFound() throws Exception {
        // Given
        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn(null);

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(validXmlWithMsgDefIdr);

        // Then
        assertNull(result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should return null when XPath mapping returns empty string")
    void shouldReturnNullWhenXPathMappingReturnsEmptyString() throws Exception {
        // Given
        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(validXmlWithMsgDefIdr);

        // Then
        assertNull(result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle MsgDefIdr with whitespace")
    void shouldHandleMsgDefIdrWithWhitespace() throws Exception {
        // Given
        String xmlWithWhitespace = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>  pacs.008.001.09  </MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithWhitespace);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle different MsgDefIdr values")
    void shouldHandleDifferentMsgDefIdrValues() throws Exception {
        // Given
        String xmlWithDifferentMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.12\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.002.001.12</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFIPmtStsRpt>" +
                "<GrpHdr>" +
                "<MsgId>BATCH456</MsgId>" +
                "</GrpHdr>" +
                "</FIToFIPmtStsRpt>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.002.001.12"))
                .thenReturn("//*[local-name()='FIToFIPmtStsRpt']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithDifferentMsgDefIdr);

        // Then
        assertNotNull(result);
        assertEquals("BATCH456", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.002.001.12");
    }

    @Test
    @DisplayName("Should handle XML with multiple MsgDefIdr elements")
    void shouldHandleXmlWithMultipleMsgDefIdrElements() throws Exception {
        // Given
        String xmlWithMultipleMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                "<AnotherMsgDefIdr>different.value</AnotherMsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithMultipleMsgDefIdr);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with empty MsgDefIdr element")
    void shouldHandleXmlWithEmptyMsgDefIdrElement() throws Exception {
        // Given
        String xmlWithEmptyMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr></MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithEmptyMsgDefIdr);

        // Then
        assertNull(result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("");
    }

    @Test
    @DisplayName("Should handle XML with self-closing MsgDefIdr element")
    void shouldHandleXmlWithSelfClosingMsgDefIdrElement() throws Exception {
        // Given
        String xmlWithSelfClosingMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr/>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithSelfClosingMsgDefIdr);

        // Then
        assertNull(result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("");
    }

    @Test
    @DisplayName("Should throw exception when XML is malformed")
    void shouldThrowExceptionWhenXmlIsMalformed() {
        // Given
        String malformedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document>" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        // When & Then
        assertThrows(Exception.class, () -> {
            batchIdXmlFieldExtractor.extractFieldByFileType(malformedXml);
        });
    }

    @Test
    @DisplayName("Should throw exception when XML is null")
    void shouldThrowExceptionWhenXmlIsNull() {
        // When & Then
        assertThrows(Exception.class, () -> {
            batchIdXmlFieldExtractor.extractFieldByFileType(null);
        });
    }

    @Test
    @DisplayName("Should throw exception when XML is empty")
    void shouldThrowExceptionWhenXmlIsEmpty() {
        // When & Then
        assertThrows(Exception.class, () -> {
            batchIdXmlFieldExtractor.extractFieldByFileType("");
        });
    }

    @Test
    @DisplayName("Should throw exception when XML contains only whitespace")
    void shouldThrowExceptionWhenXmlContainsOnlyWhitespace() {
        // When & Then
        assertThrows(Exception.class, () -> {
            batchIdXmlFieldExtractor.extractFieldByFileType("   ");
        });
    }

    @Test
    @DisplayName("Should handle XML with special characters in MsgDefIdr")
    void shouldHandleXmlWithSpecialCharactersInMsgDefIdr() throws Exception {
        // Given
        String xmlWithSpecialChars = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09!@#$%^&amp;*()</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09!@#$%^&*()"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithSpecialChars);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09!@#$%^&*()");
    }

    @Test
    @DisplayName("Should handle XML with unicode characters in MsgDefIdr")
    void shouldHandleXmlWithUnicodeCharactersInMsgDefIdr() throws Exception {
        // Given
        String xmlWithUnicode = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09中文</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09中文"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithUnicode);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09中文");
    }

    @Test
    @DisplayName("Should handle XML with very long MsgDefIdr")
    void shouldHandleXmlWithVeryLongMsgDefIdr() throws Exception {
        // Given
        StringBuilder longMsgDefIdr = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMsgDefIdr.append("pacs.008.001.09_");
        }
        String veryLongMsgDefIdr = longMsgDefIdr.toString();

        String xmlWithLongMsgDefIdr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>" + veryLongMsgDefIdr + "</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType(veryLongMsgDefIdr))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithLongMsgDefIdr);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType(veryLongMsgDefIdr);
    }

    @Test
    @DisplayName("Should handle XML with different namespace declarations")
    void shouldHandleXmlWithDifferentNamespaceDeclarations() throws Exception {
        // Given
        String xmlWithDifferentNamespace = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns:ns1=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<ns1:AppHdr>" +
                "<ns1:MsgDefIdr>pacs.008.001.09</ns1:MsgDefIdr>" +
                "</ns1:AppHdr>" +
                "<ns1:FIToFICstmrCdtTrf>" +
                "<ns1:GrpHdr>" +
                "<ns1:MsgId>BATCH123</ns1:MsgId>" +
                "</ns1:GrpHdr>" +
                "</ns1:FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithDifferentNamespace);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with nested elements")
    void shouldHandleXmlWithNestedElements() throws Exception {
        // Given
        String xmlWithNestedElements = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                "<NestedElement>" +
                "<DeeplyNested>" +
                "<MsgDefIdr>different.value</MsgDefIdr>" +
                "</DeeplyNested>" +
                "</NestedElement>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithNestedElements);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with attributes in MsgDefIdr element")
    void shouldHandleXmlWithAttributesInMsgDefIdrElement() throws Exception {
        // Given
        String xmlWithAttributes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr id=\"123\" type=\"standard\">pacs.008.001.09</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithAttributes);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with CDATA sections")
    void shouldHandleXmlWithCDATASections() throws Exception {
        // Given
        String xmlWithCDATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr><![CDATA[pacs.008.001.09]]></MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithCDATA);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with comments")
    void shouldHandleXmlWithComments() throws Exception {
        // Given
        String xmlWithComments = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<!-- This is a comment -->" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithComments);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with processing instructions")
    void shouldHandleXmlWithProcessingInstructions() throws Exception {
        // Given
        String xmlWithProcessingInstructions = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithProcessingInstructions);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09");
    }

    @Test
    @DisplayName("Should handle XML with mixed content")
    void shouldHandleXmlWithMixedContent() throws Exception {
        // Given
        String xmlWithMixedContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09<sub>sub</sub>suffix</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09subsuffix"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithMixedContent);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09subsuffix");
    }

    @Test
    @DisplayName("Should handle XML with entity references")
    void shouldHandleXmlWithEntityReferences() throws Exception {
        // Given
        String xmlWithEntities = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                "<AppHdr>" +
                "<MsgDefIdr>pacs.008.001.09&amp;extra</MsgDefIdr>" +
                "</AppHdr>" +
                "<FIToFICstmrCdtTrf>" +
                "<GrpHdr>" +
                "<MsgId>BATCH123</MsgId>" +
                "</GrpHdr>" +
                "</FIToFICstmrCdtTrf>" +
                "</Document>";

        when(batchIdXPathMap.getXPathForFileType("pacs.008.001.09&extra"))
                .thenReturn("//*[local-name()='FIToFICstmrCdtTrf']/*[local-name()='GrpHdr']/*[local-name()='MsgId']");

        // When
        String result = batchIdXmlFieldExtractor.extractFieldByFileType(xmlWithEntities);

        // Then
        assertNotNull(result);
        assertEquals("BATCH123", result);
        verify(batchIdXPathMap, times(1)).getXPathForFileType("pacs.008.001.09&extra");
    }
} 