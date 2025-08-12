package com.hdfcbank.sfmsconsumer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Response Tests")
class ResponseTest {

    @Test
    @DisplayName("Should create Response using builder with all fields")
    void shouldCreateResponseUsingBuilderWithAllFields() {
        // Given
        String status = "SUCCESS";
        String message = "Message processed successfully";

        // When
        Response response = Response.builder()
                .status(status)
                .message(message)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
    }

    @Test
    @DisplayName("Should create Response using builder with partial fields")
    void shouldCreateResponseUsingBuilderWithPartialFields() {
        // Given
        String status = "ERROR";

        // When
        Response response = Response.builder()
                .status(status)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("Should create Response using builder with no fields")
    void shouldCreateResponseUsingBuilderWithNoFields() {
        // When
        Response response = Response.builder().build();

        // Then
        assertNotNull(response);
        assertNull(response.getStatus());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("Should create Response using no-args constructor")
    void shouldCreateResponseUsingNoArgsConstructor() {
        // When
        Response response = new Response();

        // Then
        assertNotNull(response);
        assertNull(response.getStatus());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("Should create Response using all-args constructor")
    void shouldCreateResponseUsingAllArgsConstructor() {
        // Given
        String status = "SUCCESS";
        String message = "Message processed successfully";

        // When
        Response response = new Response(status, message);

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
    }

    @Test
    @DisplayName("Should set and get status correctly")
    void shouldSetAndGetStatusCorrectly() {
        // Given
        Response response = new Response();
        String status = "SUCCESS";

        // When
        response.setStatus(status);

        // Then
        assertEquals(status, response.getStatus());
    }

    @Test
    @DisplayName("Should set and get message correctly")
    void shouldSetAndGetMessageCorrectly() {
        // Given
        Response response = new Response();
        String message = "Message processed successfully";

        // When
        response.setMessage(message);

        // Then
        assertEquals(message, response.getMessage());
    }

    @Test
    @DisplayName("Should handle null values for all fields")
    void shouldHandleNullValuesForAllFields() {
        // Given
        Response response = Response.builder()
                .status(null)
                .message(null)
                .build();

        // Then
        assertNotNull(response);
        assertNull(response.getStatus());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("Should handle empty string values for all fields")
    void shouldHandleEmptyStringValuesForAllFields() {
        // Given
        Response response = Response.builder()
                .status("")
                .message("")
                .build();

        // Then
        assertNotNull(response);
        assertEquals("", response.getStatus());
        assertEquals("", response.getMessage());
    }

    @Test
    @DisplayName("Should handle whitespace values for all fields")
    void shouldHandleWhitespaceValuesForAllFields() {
        // Given
        Response response = Response.builder()
                .status("  SUCCESS  ")
                .message("  Message processed successfully  ")
                .build();

        // Then
        assertNotNull(response);
        assertEquals("  SUCCESS  ", response.getStatus());
        assertEquals("  Message processed successfully  ", response.getMessage());
    }

    @Test
    @DisplayName("Should handle special characters in all fields")
    void shouldHandleSpecialCharactersInAllFields() {
        // Given
        String status = "SUCCESS!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String message = "Message processed successfully!@#$%^&*()_+-=[]{}|;':\",./<>?";

        Response response = Response.builder()
                .status(status)
                .message(message)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
    }

    @Test
    @DisplayName("Should handle unicode characters in all fields")
    void shouldHandleUnicodeCharactersInAllFields() {
        // Given
        String status = "SUCCESS中文";
        String message = "Message processed successfully中文, हिन्दी, العربية, русский, español, français";

        Response response = Response.builder()
                .status(status)
                .message(message)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
    }

    @Test
    @DisplayName("Should handle very long values for all fields")
    void shouldHandleVeryLongValuesForAllFields() {
        // Given
        StringBuilder longStatus = new StringBuilder();
        StringBuilder longMessage = new StringBuilder();

        for (int i = 0; i < 1000; i++) {
            longStatus.append("SUCCESS_");
            longMessage.append("This is a very long message that contains a lot of text. ");
        }

        String veryLongStatus = longStatus.toString();
        String veryLongMessage = longMessage.toString();

        Response response = Response.builder()
                .status(veryLongStatus)
                .message(veryLongMessage)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(veryLongStatus, response.getStatus());
        assertEquals(veryLongMessage, response.getMessage());
    }

    @Test
    @DisplayName("Should handle different status values")
    void shouldHandleDifferentStatusValues() {
        // Given
        String[] statuses = {
            "SUCCESS",
            "ERROR",
            "PENDING",
            "PROCESSING",
            "COMPLETED",
            "FAILED",
            "TIMEOUT",
            "CANCELLED"
        };

        // When & Then
        for (String status : statuses) {
            Response response = Response.builder()
                    .status(status)
                    .build();

            assertEquals(status, response.getStatus());
        }
    }

    @Test
    @DisplayName("Should handle different message values")
    void shouldHandleDifferentMessageValues() {
        // Given
        String[] messages = {
            "Message processed successfully",
            "Message processing failed",
            "Invalid input provided",
            "Required field is missing",
            "Processing timeout",
            "Resource not found",
            "Internal server error",
            "Unauthorized access"
        };

        // When & Then
        for (String message : messages) {
            Response response = Response.builder()
                    .message(message)
                    .build();

            assertEquals(message, response.getMessage());
        }
    }

    @Test
    @DisplayName("Should update field values after creation")
    void shouldUpdateFieldValuesAfterCreation() {
        // Given
        Response response = Response.builder()
                .status("INITIAL_STATUS")
                .message("Initial message")
                .build();

        // When
        response.setStatus("UPDATED_STATUS");
        response.setMessage("Updated message");

        // Then
        assertEquals("UPDATED_STATUS", response.getStatus());
        assertEquals("Updated message", response.getMessage());
    }

    @Test
    @DisplayName("Should handle toString method")
    void shouldHandleToStringMethod() {
        // Given
        Response response = Response.builder()
                .status("SUCCESS")
                .message("Message processed successfully")
                .build();

        // When
        String toString = response.toString();

        // Then
        assertNotNull(toString);
        // Check that toString contains the class name
        assertTrue(toString.contains("Response"));
        // toString should not be empty
        assertFalse(toString.isEmpty());
        // toString should not be null
        assertNotNull(toString);
    }

    @Test
    @DisplayName("Should handle toString method with null values")
    void shouldHandleToStringMethodWithNullValues() {
        // Given
        Response response = Response.builder().build();

        // When
        String toString = response.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Response"));
    }

    @Test
    @DisplayName("Should handle toString method with empty values")
    void shouldHandleToStringMethodWithEmptyValues() {
        // Given
        Response response = Response.builder()
                .status("")
                .message("")
                .build();

        // When
        String toString = response.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("Response"));
    }

    @Test
    @DisplayName("Should create multiple Response instances with different values")
    void shouldCreateMultipleResponseInstancesWithDifferentValues() {
        // Given
        Response response1 = Response.builder()
                .status("SUCCESS")
                .message("Message processed successfully")
                .build();

        Response response2 = Response.builder()
                .status("ERROR")
                .message("Message processing failed")
                .build();

        Response response3 = Response.builder()
                .status("PENDING")
                .message("Message is being processed")
                .build();

        // Then
        assertNotNull(response1);
        assertEquals("SUCCESS", response1.getStatus());
        assertEquals("Message processed successfully", response1.getMessage());

        assertNotNull(response2);
        assertEquals("ERROR", response2.getStatus());
        assertEquals("Message processing failed", response2.getMessage());

        assertNotNull(response3);
        assertEquals("PENDING", response3.getStatus());
        assertEquals("Message is being processed", response3.getMessage());
    }

    @Test
    @DisplayName("Should handle numeric values as strings")
    void shouldHandleNumericValuesAsStrings() {
        // Given
        Response response = Response.builder()
                .status("200")
                .message("123")
                .build();

        // Then
        assertEquals("200", response.getStatus());
        assertEquals("123", response.getMessage());
    }

    @Test
    @DisplayName("Should handle boolean values as strings")
    void shouldHandleBooleanValuesAsStrings() {
        // Given
        Response response = Response.builder()
                .status("true")
                .message("false")
                .build();

        // Then
        assertEquals("true", response.getStatus());
        assertEquals("false", response.getMessage());
    }

    @Test
    @DisplayName("Should handle JSON-like values")
    void shouldHandleJsonLikeValues() {
        // Given
        String jsonStatus = "{\"status\":\"SUCCESS\"}";
        String jsonMessage = "{\"message\":\"Message processed successfully\"}";

        Response response = Response.builder()
                .status(jsonStatus)
                .message(jsonMessage)
                .build();

        // Then
        assertEquals(jsonStatus, response.getStatus());
        assertEquals(jsonMessage, response.getMessage());
    }

    @Test
    @DisplayName("Should handle XML-like values")
    void shouldHandleXmlLikeValues() {
        // Given
        String xmlStatus = "<status>SUCCESS</status>";
        String xmlMessage = "<message>Message processed successfully</message>";

        Response response = Response.builder()
                .status(xmlStatus)
                .message(xmlMessage)
                .build();

        // Then
        assertEquals(xmlStatus, response.getStatus());
        assertEquals(xmlMessage, response.getMessage());
    }

    @Test
    @DisplayName("Should handle constructor with null values")
    void shouldHandleConstructorWithNullValues() {
        // When
        Response response = new Response(null, null);

        // Then
        assertNotNull(response);
        assertNull(response.getStatus());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("Should handle constructor with empty values")
    void shouldHandleConstructorWithEmptyValues() {
        // When
        Response response = new Response("", "");

        // Then
        assertNotNull(response);
        assertEquals("", response.getStatus());
        assertEquals("", response.getMessage());
    }

    @Test
    @DisplayName("Should handle constructor with mixed null and non-null values")
    void shouldHandleConstructorWithMixedNullAndNonNullValues() {
        // When
        Response response1 = new Response("SUCCESS", null);
        Response response2 = new Response(null, "Message processed successfully");

        // Then
        assertNotNull(response1);
        assertEquals("SUCCESS", response1.getStatus());
        assertNull(response1.getMessage());

        assertNotNull(response2);
        assertNull(response2.getStatus());
        assertEquals("Message processed successfully", response2.getMessage());
    }

    @Test
    @DisplayName("Should handle object identity for different instances")
    void shouldHandleObjectIdentityForDifferentInstances() {
        // Given
        Response response1 = Response.builder()
                .status("SUCCESS")
                .message("Message processed successfully")
                .build();

        Response response2 = Response.builder()
                .status("SUCCESS")
                .message("Message processed successfully")
                .build();

        Response response3 = Response.builder()
                .status("ERROR")
                .message("Message processing failed")
                .build();

        // Then
        // Since Response doesn't have custom equals/hashCode, objects with same values are different instances
        assertNotEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertNotEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    @DisplayName("Should handle object identity with null values")
    void shouldHandleObjectIdentityWithNullValues() {
        // Given
        Response response1 = Response.builder().build();
        Response response2 = Response.builder().build();

        // Then
        // Since Response doesn't have custom equals/hashCode, objects with same null values are different instances
        assertNotEquals(response1, response2);
        assertNotEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Should handle object identity with different null combinations")
    void shouldHandleObjectIdentityWithDifferentNullCombinations() {
        // Given
        Response response1 = Response.builder()
                .status("SUCCESS")
                .build();

        Response response2 = Response.builder()
                .status("SUCCESS")
                .build();

        Response response3 = Response.builder()
                .message("Message processed successfully")
                .build();

        Response response4 = Response.builder()
                .message("Message processed successfully")
                .build();

        // Then
        // Since Response doesn't have custom equals/hashCode, all objects are different instances
        assertNotEquals(response1, response2);
        assertNotEquals(response3, response4);
        assertNotEquals(response1, response3);
    }

    @Test
    @DisplayName("Should handle real-world status scenarios")
    void shouldHandleRealWorldStatusScenarios() {
        // Given
        Response successResponse = Response.builder()
                .status("SUCCESS")
                .message("Message Processed.")
                .build();

        Response errorResponse = Response.builder()
                .status("ERROR")
                .message("Message Processing Failed")
                .build();

        Response pendingResponse = Response.builder()
                .status("PENDING")
                .message("Message is being processed")
                .build();

        // Then
        assertEquals("SUCCESS", successResponse.getStatus());
        assertEquals("Message Processed.", successResponse.getMessage());

        assertEquals("ERROR", errorResponse.getStatus());
        assertEquals("Message Processing Failed", errorResponse.getMessage());

        assertEquals("PENDING", pendingResponse.getStatus());
        assertEquals("Message is being processed", pendingResponse.getMessage());
    }

    @Test
    @DisplayName("Should handle status codes as strings")
    void shouldHandleStatusCodeAsStrings() {
        // Given
        String[] statusCodes = {
            "200", "201", "400", "401", "403", "404", "500", "502", "503"
        };

        // When & Then
        for (String statusCode : statusCodes) {
            Response response = Response.builder()
                    .status(statusCode)
                    .message("Response with status code: " + statusCode)
                    .build();

            assertEquals(statusCode, response.getStatus());
            assertEquals("Response with status code: " + statusCode, response.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle very long status and message")
    void shouldHandleVeryLongStatusAndMessage() {
        // Given
        StringBuilder longStatus = new StringBuilder();
        StringBuilder longMessage = new StringBuilder();

        for (int i = 0; i < 5000; i++) {
            longStatus.append("SUCCESS_");
            longMessage.append("This is a very long message that contains a lot of text and should be handled properly by the Response class. ");
        }

        String veryLongStatus = longStatus.toString();
        String veryLongMessage = longMessage.toString();

        Response response = Response.builder()
                .status(veryLongStatus)
                .message(veryLongMessage)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(veryLongStatus, response.getStatus());
        assertEquals(veryLongMessage, response.getMessage());
    }

    @Test
    @DisplayName("Should handle special characters in status and message")
    void shouldHandleSpecialCharactersInStatusAndMessage() {
        // Given
        String statusWithSpecialChars = "SUCCESS!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String messageWithSpecialChars = "Message with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";

        Response response = Response.builder()
                .status(statusWithSpecialChars)
                .message(messageWithSpecialChars)
                .build();

        // Then
        assertEquals(statusWithSpecialChars, response.getStatus());
        assertEquals(messageWithSpecialChars, response.getMessage());
    }

    @Test
    @DisplayName("Should handle newlines in message")
    void shouldHandleNewlinesInMessage() {
        // Given
        String messageWithNewlines = "Line 1\nLine 2\nLine 3";

        Response response = Response.builder()
                .status("SUCCESS")
                .message(messageWithNewlines)
                .build();

        // Then
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(messageWithNewlines, response.getMessage());
    }

    @Test
    @DisplayName("Should handle tabs in message")
    void shouldHandleTabsInMessage() {
        // Given
        String messageWithTabs = "Column1\tColumn2\tColumn3";

        Response response = Response.builder()
                .status("SUCCESS")
                .message(messageWithTabs)
                .build();

        // Then
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(messageWithTabs, response.getMessage());
    }
} 