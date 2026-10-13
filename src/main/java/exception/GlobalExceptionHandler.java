package com.syfe.finance.exception;
import org.springframework.dao.DataIntegrityViolationException; import org.springframework.http.*; import org.springframework.security.authentication.BadCredentialsException; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import org.springframework.http.converter.HttpMessageNotReadableException; import java.util.*;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<Map<String,String>> api(ApiException e){return ResponseEntity.status(e.getStatus()).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e){String msg=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+": "+x.getDefaultMessage()).orElse("Invalid request");return ResponseEntity.badRequest().body(Map.of("message",msg));}
 @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class}) ResponseEntity<Map<String,String>> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Invalid request: check field formats and values"));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<Map<String,String>> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("message","A resource with these details already exists"));}
 @ExceptionHandler(Exception.class) ResponseEntity<Map<String,String>> other(Exception e){return ResponseEntity.status(500).body(Map.of("message","An unexpected server error occurred"));}
}
