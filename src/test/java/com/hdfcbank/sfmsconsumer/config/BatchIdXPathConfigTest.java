package com.hdfcbank.sfmsconsumer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchIdXPathConfig Tests")
class BatchIdXPathConfigTest {

    private BatchIdXPathConfig batchIdXPathConfig;

    @BeforeEach
    void setUp() {
        batchIdXPathConfig = new BatchIdXPathConfig();
    }

    @Test
    @DisplayName("Should set and get xpaths map correctly")
    void shouldSetAndGetXpathsMap() {
        // Given
        Map<String, String> expectedXpaths = new HashMap<>();
        expectedXpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        expectedXpaths.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        expectedXpaths.put("pacs.002.001.11", "(.//*[local-name()='StsRsnInf']//*[local-name()='AddtlInf'])[1]");

        // When
        batchIdXPathConfig.setXpaths(expectedXpaths);
        Map<String, String> actualXpaths = batchIdXPathConfig.getXpaths();

        // Then
        assertNotNull(actualXpaths);
        assertEquals(expectedXpaths, actualXpaths);
        assertEquals(3, actualXpaths.size());
    }

    @Test
    @DisplayName("Should return correct XPath for existing file type")
    void shouldReturnCorrectXPathForExistingFileType() {
        // Given
        Map<String, String> xpaths = new HashMap<>();
        xpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        xpaths.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        xpaths.put("pacs.002.001.11", "(.//*[local-name()='StsRsnInf']//*[local-name()='AddtlInf'])[1]");
        xpaths.put("camt.052.001.08", ".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']");
        xpaths.put("camt.054.001.08", ".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']");
        
        batchIdXPathConfig.setXpaths(xpaths);

        // When
        String xpathForPacs008 = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");
        String xpathForPacs004 = batchIdXPathConfig.getXPathForFileType("pacs.004.001.10");
        String xpathForPacs002 = batchIdXPathConfig.getXPathForFileType("pacs.002.001.11");
        String xpathForCamt052 = batchIdXPathConfig.getXPathForFileType("camt.052.001.08");
        String xpathForCamt054 = batchIdXPathConfig.getXPathForFileType("camt.054.001.08");

        // Then
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForPacs008);
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForPacs004);
        assertEquals("(.//*[local-name()='StsRsnInf']//*[local-name()='AddtlInf'])[1]", xpathForPacs002);
        assertEquals(".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']", xpathForCamt052);
        assertEquals(".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']", xpathForCamt054);
    }

    @Test
    @DisplayName("Should return null for non-existing file type")
    void shouldReturnNullForNonExistingFileType() {
        // Given
        Map<String, String> xpaths = new HashMap<>();
        xpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpaths);

        // When
        String xpathForNonExisting = batchIdXPathConfig.getXPathForFileType("non.existing.type");

        // Then
        assertNull(xpathForNonExisting);
    }

    @Test
    @DisplayName("Should return null for null file type")
    void shouldReturnNullForNullFileType() {
        // Given
        Map<String, String> xpaths = new HashMap<>();
        xpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpaths);

        // When
        String xpathForNull = batchIdXPathConfig.getXPathForFileType(null);

        // Then
        assertNull(xpathForNull);
    }

    @Test
    @DisplayName("Should return null for empty file type")
    void shouldReturnNullForEmptyFileType() {
        // Given
        Map<String, String> xpaths = new HashMap<>();
        xpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpaths);

        // When
        String xpathForEmpty = batchIdXPathConfig.getXPathForFileType("");

        // Then
        assertNull(xpathForEmpty);
    }

    @Test
    @DisplayName("Should handle case-sensitive file type matching")
    void shouldHandleCaseSensitiveFileTypeMatching() {
        // Given
        Map<String, String> xpaths = new HashMap<>();
        xpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpaths);

        // When
        String xpathForUpperCase = batchIdXPathConfig.getXPathForFileType("PACS.008.001.09");
        String xpathForLowerCase = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");

        // Then
        assertNull(xpathForUpperCase); // Case-sensitive matching
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForLowerCase);
    }

    @Test
    @DisplayName("Should handle empty xpaths map")
    void shouldHandleEmptyXpathsMap() {
        // Given
        Map<String, String> emptyXpaths = new HashMap<>();
        batchIdXPathConfig.setXpaths(emptyXpaths);

        // When
        String xpath = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");

        // Then
        assertNull(xpath);
        assertNotNull(batchIdXPathConfig.getXpaths());
        assertTrue(batchIdXPathConfig.getXpaths().isEmpty());
    }

    @Test
    @DisplayName("Should handle null xpaths map")
    void shouldHandleNullXpathsMap() {
        // Given
        batchIdXPathConfig.setXpaths(null);

        // When
        String xpath = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");

        // Then
        assertNull(xpath);
        assertNull(batchIdXPathConfig.getXpaths());
    }

    @Test
    @DisplayName("Should handle xpaths map with null values")
    void shouldHandleXpathsMapWithNullValues() {
        // Given
        Map<String, String> xpathsWithNulls = new HashMap<>();
        xpathsWithNulls.put("pacs.008.001.09", null);
        xpathsWithNulls.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpathsWithNulls);

        // When
        String xpathForNullValue = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");
        String xpathForValidValue = batchIdXPathConfig.getXPathForFileType("pacs.004.001.10");

        // Then
        assertNull(xpathForNullValue);
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForValidValue);
    }

    @Test
    @DisplayName("Should handle xpaths map with empty string values")
    void shouldHandleXpathsMapWithEmptyStringValues() {
        // Given
        Map<String, String> xpathsWithEmptyStrings = new HashMap<>();
        xpathsWithEmptyStrings.put("pacs.008.001.09", "");
        xpathsWithEmptyStrings.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpathsWithEmptyStrings);

        // When
        String xpathForEmptyValue = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");
        String xpathForValidValue = batchIdXPathConfig.getXPathForFileType("pacs.004.001.10");

        // Then
        assertEquals("", xpathForEmptyValue);
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForValidValue);
    }

    @Test
    @DisplayName("Should handle special characters in XPath expressions")
    void shouldHandleSpecialCharactersInXPathExpressions() {
        // Given
        Map<String, String> xpathsWithSpecialChars = new HashMap<>();
        xpathsWithSpecialChars.put("test.type", "(.//*[local-name()='Test']//*[local-name()='Special'])[1]");
        xpathsWithSpecialChars.put("complex.type", ".//*[local-name()='Complex']//*[local-name()='Path']//*[local-name()='With']//*[local-name()='Special']");
        batchIdXPathConfig.setXpaths(xpathsWithSpecialChars);

        // When
        String xpathForTest = batchIdXPathConfig.getXPathForFileType("test.type");
        String xpathForComplex = batchIdXPathConfig.getXPathForFileType("complex.type");

        // Then
        assertEquals("(.//*[local-name()='Test']//*[local-name()='Special'])[1]", xpathForTest);
        assertEquals(".//*[local-name()='Complex']//*[local-name()='Path']//*[local-name()='With']//*[local-name()='Special']", xpathForComplex);
    }

    @Test
    @DisplayName("Should handle file types with special characters")
    void shouldHandleFileTypesWithSpecialCharacters() {
        // Given
        Map<String, String> xpathsWithSpecialFileTypes = new HashMap<>();
        xpathsWithSpecialFileTypes.put("test-type.with_special.chars", "(.//*[local-name()='Test'])[1]");
        xpathsWithSpecialFileTypes.put("another.type", "(.//*[local-name()='Another'])[1]");
        batchIdXPathConfig.setXpaths(xpathsWithSpecialFileTypes);

        // When
        String xpathForSpecialType = batchIdXPathConfig.getXPathForFileType("test-type.with_special.chars");
        String xpathForNormalType = batchIdXPathConfig.getXPathForFileType("another.type");

        // Then
        assertEquals("(.//*[local-name()='Test'])[1]", xpathForSpecialType);
        assertEquals("(.//*[local-name()='Another'])[1]", xpathForNormalType);
    }

    @Test
    @DisplayName("Should handle large number of file types")
    void shouldHandleLargeNumberOfFileTypes() {
        // Given
        Map<String, String> largeXpathsMap = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            largeXpathsMap.put("type." + i, "xpath." + i);
        }
        batchIdXPathConfig.setXpaths(largeXpathsMap);

        // When
        String xpathForType50 = batchIdXPathConfig.getXPathForFileType("type.50");
        String xpathForType100 = batchIdXPathConfig.getXPathForFileType("type.100");

        // Then
        assertEquals("xpath.50", xpathForType50);
        assertEquals("xpath.100", xpathForType100);
        assertEquals(100, batchIdXPathConfig.getXpaths().size());
    }

    @Test
    @DisplayName("Should handle whitespace in file types")
    void shouldHandleWhitespaceInFileTypes() {
        // Given
        Map<String, String> xpathsWithWhitespace = new HashMap<>();
        xpathsWithWhitespace.put("  pacs.008.001.09  ", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        xpathsWithWhitespace.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpathsWithWhitespace);

        // When
        String xpathForWhitespaceType = batchIdXPathConfig.getXPathForFileType("  pacs.008.001.09  ");
        String xpathForNormalType = batchIdXPathConfig.getXPathForFileType("pacs.004.001.10");

        // Then
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForWhitespaceType);
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForNormalType);
    }

    @Test
    @DisplayName("Should handle whitespace in XPath values")
    void shouldHandleWhitespaceInXPathValues() {
        // Given
        Map<String, String> xpathsWithWhitespaceValues = new HashMap<>();
        xpathsWithWhitespaceValues.put("pacs.008.001.09", "  (.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]  ");
        xpathsWithWhitespaceValues.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        batchIdXPathConfig.setXpaths(xpathsWithWhitespaceValues);

        // When
        String xpathForWhitespaceValue = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");
        String xpathForNormalValue = batchIdXPathConfig.getXPathForFileType("pacs.004.001.10");

        // Then
        assertEquals("  (.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]  ", xpathForWhitespaceValue);
        assertEquals("(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]", xpathForNormalValue);
    }

    @Test
    @DisplayName("Should handle duplicate file types (last one wins)")
    void shouldHandleDuplicateFileTypes() {
        // Given
        Map<String, String> xpathsWithDuplicates = new HashMap<>();
        xpathsWithDuplicates.put("pacs.008.001.09", "first.xpath");
        xpathsWithDuplicates.put("pacs.008.001.09", "second.xpath"); // This will override the first one
        batchIdXPathConfig.setXpaths(xpathsWithDuplicates);

        // When
        String xpath = batchIdXPathConfig.getXPathForFileType("pacs.008.001.09");

        // Then
        assertEquals("second.xpath", xpath);
        assertEquals(1, batchIdXPathConfig.getXpaths().size());
    }

    @Test
    @DisplayName("Should handle all supported file types from configuration")
    void shouldHandleAllSupportedFileTypesFromConfiguration() {
        // Given
        Map<String, String> allSupportedXpaths = new HashMap<>();
        allSupportedXpaths.put("pacs.008.001.09", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        allSupportedXpaths.put("pacs.004.001.10", "(.//*[local-name()='RmtInf']//*[local-name()='Ustrd'])[1]");
        allSupportedXpaths.put("pacs.002.001.11", "(.//*[local-name()='StsRsnInf']//*[local-name()='AddtlInf'])[1]");
        allSupportedXpaths.put("camt.052.001.08", ".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']");
        allSupportedXpaths.put("camt.054.001.08", ".//*[local-name()='GrpHdr']//*[local-name()='AddtInf']");
        batchIdXPathConfig.setXpaths(allSupportedXpaths);

        // When & Then
        assertNotNull(batchIdXPathConfig.getXPathForFileType("pacs.008.001.09"));
        assertNotNull(batchIdXPathConfig.getXPathForFileType("pacs.004.001.10"));
        assertNotNull(batchIdXPathConfig.getXPathForFileType("pacs.002.001.11"));
        assertNotNull(batchIdXPathConfig.getXPathForFileType("camt.052.001.08"));
        assertNotNull(batchIdXPathConfig.getXPathForFileType("camt.054.001.08"));
        
        assertEquals(5, batchIdXPathConfig.getXpaths().size());
    }
} 