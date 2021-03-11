package com.gemini.jobcoin.helper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.exception.JsonRequestException;
import org.junit.Assert;
import org.junit.Test;

public class ValidatorTest {

    @Test
    public void testInvalid() {
        String throwError1 = null;
        try {
            Validator.stringField("sample", JsonNodeFactory.instance.arrayNode());
        } catch (JsonRequestException e) {
            throwError1 = e.getMessage();
        }
        Assert.assertEquals(throwError1, "Payload is not json object");

        String throwError2 = null;
        try {
            Validator.stringField("sample", JsonNodeFactory.instance.objectNode());
        } catch (JsonRequestException e) {
            throwError2 = e.getMessage();
        }
        Assert.assertEquals(throwError2, "Payload missing [sample]");

        String throwError3 = null;
        try {
            Validator.stringField("sample", JsonNodeFactory.instance.objectNode()
                    .put("sample", true));
        } catch (JsonRequestException e) {
            throwError3 = e.getMessage();
        }
        Assert.assertEquals(throwError3, "[sample] needs to be a string");

        String throwError4 = null;
        try {
            Validator.stringField("sample", JsonNodeFactory.instance.objectNode()
                    .put("sample", ""));
        } catch (JsonRequestException e) {
            throwError4 = e.getMessage();
        }
        Assert.assertEquals(throwError4, "[sample] is an empty string");
    }

    @Test
    public void valid() throws JsonRequestException {
        final String value = Validator.stringField("sample", JsonNodeFactory.instance.objectNode()
                .put("sample", "value"));
        Assert.assertEquals(value, "value");
    }
}
