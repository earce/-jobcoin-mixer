package com.gemini.jobcoin.exception;

public class GeminiRequestException extends Exception {

    private final int statusCode;

    public GeminiRequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
