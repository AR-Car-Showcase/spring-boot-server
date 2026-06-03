package com.arcarshowcaseserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotVerified(AccountNotVerifiedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", "ACCOUNT_NOT_VERIFIED");
        response.put("message", ex.getMessage());
        response.put("email", ex.getEmail());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
