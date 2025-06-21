package com.example.demo.exceptions;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkerOverloadedException.class)
    public ResponseEntity<?> handleWorkerOverload(WorkerOverloadedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "WORKER_OVERLOADED",
                "message", ex.getMessage(),
                "options", new String[] { "ASSIGN_TO_OTHER", "FORCE_ASSIGN" }));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<String> handleUsernameExists(UsernameAlreadyExistsException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthExc.class)
    public ResponseEntity<String> handleAuthEx(UsernameAlreadyExistsException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return new ResponseEntity<>("Internal server error in global ex", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
