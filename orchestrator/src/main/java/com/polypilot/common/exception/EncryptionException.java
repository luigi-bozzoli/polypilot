package com.polypilot.common.exception;

public class EncryptionException extends RuntimeException {
    public EncryptionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
