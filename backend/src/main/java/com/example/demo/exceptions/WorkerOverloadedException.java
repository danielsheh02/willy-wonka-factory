package com.example.demo.exceptions;

public class WorkerOverloadedException extends RuntimeException {
    public WorkerOverloadedException(String message) {
        super(message);
    }
}