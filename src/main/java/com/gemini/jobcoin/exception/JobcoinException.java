package com.gemini.jobcoin.exception;

public class JobcoinException extends Exception {

    private final int statusCode;

    public JobcoinException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
