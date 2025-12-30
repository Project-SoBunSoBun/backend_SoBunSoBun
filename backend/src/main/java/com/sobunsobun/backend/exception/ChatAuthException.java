package com.sobunsobun.backend.exception;

public class ChatAuthException extends RuntimeException {
    public ChatAuthException(String message) {
        super(message);
    }
}