package com.gemini.jobcoin.exception;

public class JsonRequestException extends Exception {

    private final int statusCode;

    public JsonRequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
