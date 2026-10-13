
package com.syfe.finance.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void apiExceptionReturnsItsStatusAndMessage() {
        ApiException exception =
                new ApiException(HttpStatus.NOT_FOUND, "Resource not found");

        ResponseEntity<Map<String, String>> response = handler.api(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Resource not found", response.getBody().get("message"));
    }

    @Test
    void apiExceptionPreservesConflictStatus() {
        ApiException exception =
                new ApiException(HttpStatus.CONFLICT, "Already exists");

        ResponseEntity<Map<String, String>> response = handler.api(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Already exists", response.getBody().get("message"));
    }

    @Test
    void validationExceptionReturnsFirstFieldError() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");

        bindingResult.addError(
                new FieldError("request", "username", "must not be blank"));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response =
                handler.validation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "username: must not be blank",
                response.getBody().get("message"));
    }

    @Test
    void validationExceptionWithoutFieldErrorsReturnsFallbackMessage() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response =
                handler.validation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody().get("message"));
    }

    @Test
    void unreadableRequestReturnsBadRequest() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(
                        "Invalid JSON", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<Map<String, String>> response = handler.bad(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Invalid request: check field formats and values",
                response.getBody().get("message"));
    }

    @Test
    void illegalArgumentReturnsBadRequest() {
        IllegalArgumentException exception =
                new IllegalArgumentException("Invalid argument");

        ResponseEntity<Map<String, String>> response = handler.bad(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Invalid request: check field formats and values",
                response.getBody().get("message"));
    }

    @Test
    void dataIntegrityViolationReturnsConflict() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Duplicate record");

        ResponseEntity<Map<String, String>> response =
                handler.conflict(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "A resource with these details already exists",
                response.getBody().get("message"));
    }

    @Test
    void unexpectedExceptionReturnsInternalServerError() {
        Exception exception = new Exception("Internal detail");

        ResponseEntity<Map<String, String>> response = handler.other(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                "An unexpected server error occurred",
                response.getBody().get("message"));
        assertFalse(
                response.getBody().get("message").contains("Internal detail"));
    }

    @Test
    void apiExceptionStoresItsStatus() {
        ApiException exception =
                new ApiException(HttpStatus.FORBIDDEN, "Access denied");

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Access denied", exception.getMessage());
    }
}
