package com.gemini.jobcoin.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gemini.jobcoin.exception.JsonRequestException;

public class Validator {

    public static String stringField(final String addressParam, final JsonNode msg)
            throws JsonRequestException {

        if (!(msg instanceof ObjectNode)) {
            throw new JsonRequestException("Payload is not json object", 400);
        }
        if (!msg.has(addressParam)) {
            throw new JsonRequestException(String.format("Payload missing [%s]", addressParam), 400);
        }
        if (!msg.get(addressParam).isTextual()) {
            throw new JsonRequestException(String.format("[%s] needs to be a string", addressParam), 400);
        }
        final String addr = msg.get(addressParam).textValue();
        if (addr.length() < 1) {
            throw new JsonRequestException(String.format("[%s] is an empty string", addressParam), 400);
        }
        return addr;
    }
}
