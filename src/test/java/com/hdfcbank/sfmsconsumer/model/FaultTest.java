package com.hdfcbank.sfmsconsumer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fault Tests")
class FaultTest {

    @Test
    @DisplayName("Should create Fault using builder with all fields")
    void shouldCreateFaultUsingBuilderWithAllFields() {
        // Given
        String errorType = "VALIDATION_ERROR";
        String responseStatusCode = "400";
        String errorCode = "INVALID_INPUT";
        String errorDescription = "Invalid input provided";

        // When
        Fault fault = Fault.builder()
                .errorType(errorType)
                .responseStatusCode(responseStatusCode)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .build();

        // Then
        assertNotNull(fault);
        assertEquals(errorType, fault.getErrorType());
        assertEquals(responseStatusCode, fault.getResponseStatusCode());
        assertEquals(errorCode, fault.getErrorCode());
        assertEquals(errorDescription, fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should create Fault using builder with partial fields")
    void shouldCreateFaultUsingBuilderWithPartialFields() {
        // Given
        String errorType = "PROCESSING_ERROR";
        String errorCode = "INTERNAL_ERROR";

        // When
        Fault fault = Fault.builder()
                .errorType(errorType)
                .errorCode(errorCode)
                .build();

        // Then
        assertNotNull(fault);
        assertEquals(errorType, fault.getErrorType());
        assertEquals(errorCode, fault.getErrorCode());
        assertNull(fault.getResponseStatusCode());
        assertNull(fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should create Fault using builder with no fields")
    void shouldCreateFaultUsingBuilderWithNoFields() {
        // When
        Fault fault = Fault.builder().build();

        // Then
        assertNotNull(fault);
        assertNull(fault.getErrorType());
        assertNull(fault.getResponseStatusCode());
        assertNull(fault.getErrorCode());
        assertNull(fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should set and get errorType correctly")
    void shouldSetAndGetErrorTypeCorrectly() {
        // Given
        Fault fault = Fault.builder().build();
        String errorType = "VALIDATION_ERROR";

        // When
        fault.setErrorType(errorType);

        // Then
        assertEquals(errorType, fault.getErrorType());
    }

    @Test
    @DisplayName("Should set and get responseStatusCode correctly")
    void shouldSetAndGetResponseStatusCodeCorrectly() {
        // Given
        Fault fault = Fault.builder().build();
        String responseStatusCode = "500";

        // When
        fault.setResponseStatusCode(responseStatusCode);

        // Then
        assertEquals(responseStatusCode, fault.getResponseStatusCode());
    }

    @Test
    @DisplayName("Should set and get errorCode correctly")
    void shouldSetAndGetErrorCodeCorrectly() {
        // Given
        Fault fault = Fault.builder().build();
        String errorCode = "INVALID_INPUT";

        // When
        fault.setErrorCode(errorCode);

        // Then
        assertEquals(errorCode, fault.getErrorCode());
    }

    @Test
    @DisplayName("Should set and get errorDescription correctly")
    void shouldSetAndGetErrorDescriptionCorrectly() {
        // Given
        Fault fault = Fault.builder().build();
        String errorDescription = "Invalid input provided";

        // When
        fault.setErrorDescription(errorDescription);

        // Then
        assertEquals(errorDescription, fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle null values for all fields")
    void shouldHandleNullValuesForAllFields() {
        // Given
        Fault fault = Fault.builder()
                .errorType(null)
                .responseStatusCode(null)
                .errorCode(null)
                .errorDescription(null)
                .build();

        // Then
        assertNotNull(fault);
        assertNull(fault.getErrorType());
        assertNull(fault.getResponseStatusCode());
        assertNull(fault.getErrorCode());
        assertNull(fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle empty string values for all fields")
    void shouldHandleEmptyStringValuesForAllFields() {
        // Given
        Fault fault = Fault.builder()
                .errorType("")
                .responseStatusCode("")
                .errorCode("")
                .errorDescription("")
                .build();

        // Then
        assertNotNull(fault);
        assertEquals("", fault.getErrorType());
        assertEquals("", fault.getResponseStatusCode());
        assertEquals("", fault.getErrorCode());
        assertEquals("", fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle whitespace values for all fields")
    void shouldHandleWhitespaceValuesForAllFields() {
        // Given
        Fault fault = Fault.builder()
                .errorType("  VALIDATION_ERROR  ")
                .responseStatusCode("  400  ")
                .errorCode("  INVALID_INPUT  ")
                .errorDescription("  Invalid input provided  ")
                .build();

        // Then
        assertNotNull(fault);
        assertEquals("  VALIDATION_ERROR  ", fault.getErrorType());
        assertEquals("  400  ", fault.getResponseStatusCode());
        assertEquals("  INVALID_INPUT  ", fault.getErrorCode());
        assertEquals("  Invalid input provided  ", fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle special characters in all fields")
    void shouldHandleSpecialCharactersInAllFields() {
        // Given
        String errorType = "VALIDATION_ERROR!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String responseStatusCode = "400!@#$%^&*()";
        String errorCode = "INVALID_INPUT!@#$%^&*()";
        String errorDescription = "Invalid input provided!@#$%^&*()_+-=[]{}|;':\",./<>?";

        Fault fault = Fault.builder()
                .errorType(errorType)
                .responseStatusCode(responseStatusCode)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .build();

        // Then
        assertNotNull(fault);
        assertEquals(errorType, fault.getErrorType());
        assertEquals(responseStatusCode, fault.getResponseStatusCode());
        assertEquals(errorCode, fault.getErrorCode());
        assertEquals(errorDescription, fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle unicode characters in all fields")
    void shouldHandleUnicodeCharactersInAllFields() {
        // Given
        String errorType = "VALIDATION_ERROR中文";
        String responseStatusCode = "400हिन्दी";
        String errorCode = "INVALID_INPUTالعربية";
        String errorDescription = "Invalid input provided中文, हिन्दी, العربية, русский, español, français";

        Fault fault = Fault.builder()
                .errorType(errorType)
                .responseStatusCode(responseStatusCode)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .build();

        // Then
        assertNotNull(fault);
        assertEquals(errorType, fault.getErrorType());
        assertEquals(responseStatusCode, fault.getResponseStatusCode());
        assertEquals(errorCode, fault.getErrorCode());
        assertEquals(errorDescription, fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle very long values for all fields")
    void shouldHandleVeryLongValuesForAllFields() {
        // Given
        StringBuilder longErrorType = new StringBuilder();
        StringBuilder longResponseStatusCode = new StringBuilder();
        StringBuilder longErrorCode = new StringBuilder();
        StringBuilder longErrorDescription = new StringBuilder();

        for (int i = 0; i < 1000; i++) {
            longErrorType.append("VALIDATION_ERROR_");
            longResponseStatusCode.append("400_");
            longErrorCode.append("INVALID_INPUT_");
            longErrorDescription.append("This is a very long error description that contains a lot of text. ");
        }

        String veryLongErrorType = longErrorType.toString();
        String veryLongResponseStatusCode = longResponseStatusCode.toString();
        String veryLongErrorCode = longErrorCode.toString();
        String veryLongErrorDescription = longErrorDescription.toString();

        Fault fault = Fault.builder()
                .errorType(veryLongErrorType)
                .responseStatusCode(veryLongResponseStatusCode)
                .errorCode(veryLongErrorCode)
                .errorDescription(veryLongErrorDescription)
                .build();

        // Then
        assertNotNull(fault);
        assertEquals(veryLongErrorType, fault.getErrorType());
        assertEquals(veryLongResponseStatusCode, fault.getResponseStatusCode());
        assertEquals(veryLongErrorCode, fault.getErrorCode());
        assertEquals(veryLongErrorDescription, fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle different error types")
    void shouldHandleDifferentErrorTypes() {
        // Given
        String[] errorTypes = {
            "VALIDATION_ERROR",
            "PROCESSING_ERROR",
            "AUTHORIZATION_ERROR",
            "NETWORK_ERROR",
            "TIMEOUT_ERROR",
            "RESOURCE_NOT_FOUND",
            "INTERNAL_SERVER_ERROR"
        };

        // When & Then
        for (String errorType : errorTypes) {
            Fault fault = Fault.builder()
                    .errorType(errorType)
                    .build();

            assertEquals(errorType, fault.getErrorType());
        }
    }

    @Test
    @DisplayName("Should handle different HTTP status codes")
    void shouldHandleDifferentHttpStatusCodes() {
        // Given
        String[] statusCodes = {
            "200", "201", "400", "401", "403", "404", "500", "502", "503"
        };

        // When & Then
        for (String statusCode : statusCodes) {
            Fault fault = Fault.builder()
                    .responseStatusCode(statusCode)
                    .build();

            assertEquals(statusCode, fault.getResponseStatusCode());
        }
    }

    @Test
    @DisplayName("Should handle different error codes")
    void shouldHandleDifferentErrorCodes() {
        // Given
        String[] errorCodes = {
            "INVALID_INPUT",
            "MISSING_REQUIRED_FIELD",
            "INVALID_FORMAT",
            "VALUE_TOO_LONG",
            "VALUE_TOO_SHORT",
            "INVALID_DATE",
            "INVALID_EMAIL",
            "DUPLICATE_ENTRY"
        };

        // When & Then
        for (String errorCode : errorCodes) {
            Fault fault = Fault.builder()
                    .errorCode(errorCode)
                    .build();

            assertEquals(errorCode, fault.getErrorCode());
        }
    }

    @Test
    @DisplayName("Should handle different error descriptions")
    void shouldHandleDifferentErrorDescriptions() {
        // Given
        String[] errorDescriptions = {
            "Invalid input provided",
            "Required field is missing",
            "Invalid format for the provided data",
            "Value exceeds maximum length",
            "Value is below minimum length",
            "Invalid date format",
            "Invalid email format",
            "Duplicate entry found"
        };

        // When & Then
        for (String errorDescription : errorDescriptions) {
            Fault fault = Fault.builder()
                    .errorDescription(errorDescription)
                    .build();

            assertEquals(errorDescription, fault.getErrorDescription());
        }
    }

    @Test
    @DisplayName("Should update field values after creation")
    void shouldUpdateFieldValuesAfterCreation() {
        // Given
        Fault fault = Fault.builder()
                .errorType("INITIAL_ERROR")
                .responseStatusCode("400")
                .errorCode("INITIAL_CODE")
                .errorDescription("Initial description")
                .build();

        // When
        fault.setErrorType("UPDATED_ERROR");
        fault.setResponseStatusCode("500");
        fault.setErrorCode("UPDATED_CODE");
        fault.setErrorDescription("Updated description");

        // Then
        assertEquals("UPDATED_ERROR", fault.getErrorType());
        assertEquals("500", fault.getResponseStatusCode());
        assertEquals("UPDATED_CODE", fault.getErrorCode());
        assertEquals("Updated description", fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle toString method")
    void shouldHandleToStringMethod() {
        // Given
        Fault fault = Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build();

        // When
        String toString = fault.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("VALIDATION_ERROR"));
        assertTrue(toString.contains("400"));
        assertTrue(toString.contains("INVALID_INPUT"));
        assertTrue(toString.contains("Invalid input provided"));
    }

    @Test
    @DisplayName("Should handle toString method with null values")
    void shouldHandleToStringMethodWithNullValues() {
        // Given
        Fault fault = Fault.builder().build();

        // When
        String toString = fault.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Fault"));
    }

    @Test
    @DisplayName("Should handle toString method with empty values")
    void shouldHandleToStringMethodWithEmptyValues() {
        // Given
        Fault fault = Fault.builder()
                .errorType("")
                .responseStatusCode("")
                .errorCode("")
                .errorDescription("")
                .build();

        // When
        String toString = fault.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Fault"));
    }

    @Test
    @DisplayName("Should create multiple Fault instances with different values")
    void shouldCreateMultipleFaultInstancesWithDifferentValues() {
        // Given
        Fault fault1 = Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build();

        Fault fault2 = Fault.builder()
                .errorType("PROCESSING_ERROR")
                .responseStatusCode("500")
                .errorCode("INTERNAL_ERROR")
                .errorDescription("Internal processing error")
                .build();

        Fault fault3 = Fault.builder()
                .errorType("AUTHORIZATION_ERROR")
                .responseStatusCode("401")
                .errorCode("UNAUTHORIZED")
                .errorDescription("User not authorized")
                .build();

        // Then
        assertNotNull(fault1);
        assertEquals("VALIDATION_ERROR", fault1.getErrorType());
        assertEquals("400", fault1.getResponseStatusCode());

        assertNotNull(fault2);
        assertEquals("PROCESSING_ERROR", fault2.getErrorType());
        assertEquals("500", fault2.getResponseStatusCode());

        assertNotNull(fault3);
        assertEquals("AUTHORIZATION_ERROR", fault3.getErrorType());
        assertEquals("401", fault3.getResponseStatusCode());
    }

    @Test
    @DisplayName("Should handle numeric values as strings")
    void shouldHandleNumericValuesAsStrings() {
        // Given
        Fault fault = Fault.builder()
                .errorType("123")
                .responseStatusCode("456")
                .errorCode("789")
                .errorDescription("012")
                .build();

        // Then
        assertEquals("123", fault.getErrorType());
        assertEquals("456", fault.getResponseStatusCode());
        assertEquals("789", fault.getErrorCode());
        assertEquals("012", fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle boolean values as strings")
    void shouldHandleBooleanValuesAsStrings() {
        // Given
        Fault fault = Fault.builder()
                .errorType("true")
                .responseStatusCode("false")
                .errorCode("TRUE")
                .errorDescription("FALSE")
                .build();

        // Then
        assertEquals("true", fault.getErrorType());
        assertEquals("false", fault.getResponseStatusCode());
        assertEquals("TRUE", fault.getErrorCode());
        assertEquals("FALSE", fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle JSON-like values")
    void shouldHandleJsonLikeValues() {
        // Given
        String jsonErrorType = "{\"type\":\"VALIDATION_ERROR\"}";
        String jsonResponseStatusCode = "{\"code\":\"400\"}";
        String jsonErrorCode = "{\"code\":\"INVALID_INPUT\"}";
        String jsonErrorDescription = "{\"description\":\"Invalid input provided\"}";

        Fault fault = Fault.builder()
                .errorType(jsonErrorType)
                .responseStatusCode(jsonResponseStatusCode)
                .errorCode(jsonErrorCode)
                .errorDescription(jsonErrorDescription)
                .build();

        // Then
        assertEquals(jsonErrorType, fault.getErrorType());
        assertEquals(jsonResponseStatusCode, fault.getResponseStatusCode());
        assertEquals(jsonErrorCode, fault.getErrorCode());
        assertEquals(jsonErrorDescription, fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle XML-like values")
    void shouldHandleXmlLikeValues() {
        // Given
        String xmlErrorType = "<errorType>VALIDATION_ERROR</errorType>";
        String xmlResponseStatusCode = "<responseStatusCode>400</responseStatusCode>";
        String xmlErrorCode = "<errorCode>INVALID_INPUT</errorCode>";
        String xmlErrorDescription = "<errorDescription>Invalid input provided</errorDescription>";

        Fault fault = Fault.builder()
                .errorType(xmlErrorType)
                .responseStatusCode(xmlResponseStatusCode)
                .errorCode(xmlErrorCode)
                .errorDescription(xmlErrorDescription)
                .build();

        // Then
        assertEquals(xmlErrorType, fault.getErrorType());
        assertEquals(xmlResponseStatusCode, fault.getResponseStatusCode());
        assertEquals(xmlErrorCode, fault.getErrorCode());
        assertEquals(xmlErrorDescription, fault.getErrorDescription());
    }
} 