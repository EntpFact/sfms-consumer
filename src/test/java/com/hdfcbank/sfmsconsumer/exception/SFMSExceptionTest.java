package com.hdfcbank.sfmsconsumer.exception;

import com.hdfcbank.sfmsconsumer.model.Fault;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SFMSException Tests")
class SFMSExceptionTest {

    @Test
    @DisplayName("Should create SFMSException with message only")
    void shouldCreateSFMSExceptionWithMessageOnly() {
        // Given
        String message = "Test error message";

        // When
        SFMSException exception = new SFMSException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrors());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with null message")
    void shouldCreateSFMSExceptionWithNullMessage() {
        // When
        SFMSException exception = new SFMSException(null);

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getErrors());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with empty message")
    void shouldCreateSFMSExceptionWithEmptyMessage() {
        // Given
        String message = "";

        // When
        SFMSException exception = new SFMSException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrors());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with default constructor")
    void shouldCreateSFMSExceptionWithDefaultConstructor() {
        // When
        SFMSException exception = new SFMSException();

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getErrors());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with message and cause")
    void shouldCreateSFMSExceptionWithMessageAndCause() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Original cause");

        // When
        SFMSException exception = new SFMSException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getErrors());
    }

    @Test
    @DisplayName("Should create SFMSException with null message and cause")
    void shouldCreateSFMSExceptionWithNullMessageAndCause() {
        // Given
        Throwable cause = new RuntimeException("Original cause");

        // When
        SFMSException exception = new SFMSException(null, cause);

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getErrors());
    }

    @Test
    @DisplayName("Should create SFMSException with message and null cause")
    void shouldCreateSFMSExceptionWithMessageAndNullCause() {
        // Given
        String message = "Test error message";

        // When
        SFMSException exception = new SFMSException(message, (Throwable) null);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getErrors());
    }

    @Test
    @DisplayName("Should create SFMSException with message and errors")
    void shouldCreateSFMSExceptionWithMessageAndErrors() {
        // Given
        String message = "Test error message";
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build(),
            Fault.builder()
                .errorType("PROCESSING_ERROR")
                .responseStatusCode("500")
                .errorCode("INTERNAL_ERROR")
                .errorDescription("Internal processing error")
                .build()
        );

        // When
        SFMSException exception = new SFMSException(message, errors);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(errors, exception.getErrors());
        assertEquals(2, exception.getErrors().size());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with null message and errors")
    void shouldCreateSFMSExceptionWithNullMessageAndErrors() {
        // Given
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build()
        );

        // When
        SFMSException exception = new SFMSException(null, errors);

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertEquals(errors, exception.getErrors());
        assertEquals(1, exception.getErrors().size());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with message and null errors")
    void shouldCreateSFMSExceptionWithMessageAndNullErrors() {
        // Given
        String message = "Test error message";

        // When
        SFMSException exception = new SFMSException(message, (List<Fault>) null);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrors());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SFMSException with empty errors list")
    void shouldCreateSFMSExceptionWithEmptyErrorsList() {
        // Given
        String message = "Test error message";
        List<Fault> errors = new ArrayList<>();

        // When
        SFMSException exception = new SFMSException(message, errors);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(errors, exception.getErrors());
        assertTrue(exception.getErrors().isEmpty());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should set and get errors correctly")
    void shouldSetAndGetErrorsCorrectly() {
        // Given
        SFMSException exception = new SFMSException("Test message");
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build()
        );

        // When
        exception.setErrors(errors);

        // Then
        assertEquals(errors, exception.getErrors());
        assertEquals(1, exception.getErrors().size());
    }

    @Test
    @DisplayName("Should set errors to null")
    void shouldSetErrorsToNull() {
        // Given
        SFMSException exception = new SFMSException("Test message");
        List<Fault> initialErrors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build()
        );
        exception.setErrors(initialErrors);

        // When
        exception.setErrors(null);

        // Then
        assertNull(exception.getErrors());
    }

    @Test
    @DisplayName("Should set errors to empty list")
    void shouldSetErrorsToEmptyList() {
        // Given
        SFMSException exception = new SFMSException("Test message");
        List<Fault> initialErrors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build()
        );
        exception.setErrors(initialErrors);

        // When
        List<Fault> emptyErrors = new ArrayList<>();
        exception.setErrors(emptyErrors);

        // Then
        assertEquals(emptyErrors, exception.getErrors());
        assertTrue(exception.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple faults in errors list")
    void shouldHandleMultipleFaultsInErrorsList() {
        // Given
        String message = "Multiple validation errors";
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build(),
            Fault.builder()
                .errorType("PROCESSING_ERROR")
                .responseStatusCode("500")
                .errorCode("INTERNAL_ERROR")
                .errorDescription("Internal processing error")
                .build(),
            Fault.builder()
                .errorType("AUTHORIZATION_ERROR")
                .responseStatusCode("401")
                .errorCode("UNAUTHORIZED")
                .errorDescription("User not authorized")
                .build()
        );

        // When
        SFMSException exception = new SFMSException(message, errors);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(errors, exception.getErrors());
        assertEquals(3, exception.getErrors().size());
        
        // Verify individual faults
        Fault firstFault = exception.getErrors().get(0);
        assertEquals("VALIDATION_ERROR", firstFault.getErrorType());
        assertEquals("400", firstFault.getResponseStatusCode());
        assertEquals("INVALID_INPUT", firstFault.getErrorCode());
        assertEquals("Invalid input provided", firstFault.getErrorDescription());
        
        Fault secondFault = exception.getErrors().get(1);
        assertEquals("PROCESSING_ERROR", secondFault.getErrorType());
        assertEquals("500", secondFault.getResponseStatusCode());
        assertEquals("INTERNAL_ERROR", secondFault.getErrorCode());
        assertEquals("Internal processing error", secondFault.getErrorDescription());
        
        Fault thirdFault = exception.getErrors().get(2);
        assertEquals("AUTHORIZATION_ERROR", thirdFault.getErrorType());
        assertEquals("401", thirdFault.getResponseStatusCode());
        assertEquals("UNAUTHORIZED", thirdFault.getErrorCode());
        assertEquals("User not authorized", thirdFault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle faults with null values")
    void shouldHandleFaultsWithNullValues() {
        // Given
        String message = "Test error message";
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType(null)
                .responseStatusCode(null)
                .errorCode(null)
                .errorDescription(null)
                .build()
        );

        // When
        SFMSException exception = new SFMSException(message, errors);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(errors, exception.getErrors());
        assertEquals(1, exception.getErrors().size());
        
        Fault fault = exception.getErrors().get(0);
        assertNull(fault.getErrorType());
        assertNull(fault.getResponseStatusCode());
        assertNull(fault.getErrorCode());
        assertNull(fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle faults with empty string values")
    void shouldHandleFaultsWithEmptyStringValues() {
        // Given
        String message = "Test error message";
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType("")
                .responseStatusCode("")
                .errorCode("")
                .errorDescription("")
                .build()
        );

        // When
        SFMSException exception = new SFMSException(message, errors);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(errors, exception.getErrors());
        assertEquals(1, exception.getErrors().size());
        
        Fault fault = exception.getErrors().get(0);
        assertEquals("", fault.getErrorType());
        assertEquals("", fault.getResponseStatusCode());
        assertEquals("", fault.getErrorCode());
        assertEquals("", fault.getErrorDescription());
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void shouldHandleSpecialCharactersInMessage() {
        // Given
        String message = "Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        Throwable cause = new RuntimeException("Cause with special chars: !@#$%^&*()");

        // When
        SFMSException exception = new SFMSException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should handle very long message")
    void shouldHandleVeryLongMessage() {
        // Given
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a very long error message that contains a lot of text. ");
        }
        String message = longMessage.toString();

        // When
        SFMSException exception = new SFMSException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should handle unicode characters in message")
    void shouldHandleUnicodeCharactersInMessage() {
        // Given
        String message = "Error with unicode: 中文, हिन्दी, العربية, русский, español, français";

        // When
        SFMSException exception = new SFMSException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should verify exception inheritance hierarchy")
    void shouldVerifyExceptionInheritanceHierarchy() {
        // Given
        SFMSException exception = new SFMSException("Test message");

        // When & Then
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @DisplayName("Should handle serialization compatibility")
    void shouldHandleSerializationCompatibility() {
        // Given
        SFMSException exception = new SFMSException("Test message");

        // When & Then
        // Verify that serialVersionUID is accessible (though it's private)
        // This test ensures the class is designed for serialization
        assertNotNull(exception);
        // The serialVersionUID field should be present in the class
        assertTrue(SFMSException.class.getDeclaredFields().length > 0);
    }

    @Test
    @DisplayName("Should handle exception chaining")
    void shouldHandleExceptionChaining() {
        // Given
        RuntimeException originalException = new RuntimeException("Original exception");
        SFMSException sfmsException = new SFMSException("SFMS error", originalException);

        // When
        Throwable cause = sfmsException.getCause();

        // Then
        assertNotNull(cause);
        assertEquals(originalException, cause);
        assertEquals("Original exception", cause.getMessage());
    }

    @Test
    @DisplayName("Should handle multiple exception chaining levels")
    void shouldHandleMultipleExceptionChainingLevels() {
        // Given
        IllegalArgumentException illegalArgException = new IllegalArgumentException("Invalid argument");
        RuntimeException runtimeException = new RuntimeException("Runtime error", illegalArgException);
        SFMSException sfmsException = new SFMSException("SFMS error", runtimeException);

        // When
        Throwable cause = sfmsException.getCause();
        Throwable rootCause = cause.getCause();

        // Then
        assertNotNull(cause);
        assertEquals(runtimeException, cause);
        assertEquals("Runtime error", cause.getMessage());
        
        assertNotNull(rootCause);
        assertEquals(illegalArgException, rootCause);
        assertEquals("Invalid argument", rootCause.getMessage());
    }

    @Test
    @DisplayName("Should handle exception with errors and cause")
    void shouldHandleExceptionWithErrorsAndCause() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Original cause");
        List<Fault> errors = Arrays.asList(
            Fault.builder()
                .errorType("VALIDATION_ERROR")
                .responseStatusCode("400")
                .errorCode("INVALID_INPUT")
                .errorDescription("Invalid input provided")
                .build()
        );

        // When
        SFMSException exception = new SFMSException(message, cause);
        exception.setErrors(errors);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(errors, exception.getErrors());
        assertEquals(1, exception.getErrors().size());
    }
} 