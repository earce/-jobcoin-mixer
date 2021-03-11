package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.exception.GeminiRequestException;
import com.gemini.jobcoin.external.http.GeminiClient;
import io.vertx.core.eventbus.Message;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class BalanceHandlerTest {

    @Test
    public void testJsonError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);
        final BalanceHandler balanceHandler = new BalanceHandler(geminiClient);

        doReturn("junkjson")
                .when(message)
                .body();

        balanceHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue processing Json")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testNullError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final BalanceHandler balanceHandler = new BalanceHandler(null);

        doReturn(JsonNodeFactory.instance.objectNode().put("address", "testaddress1").toString())
                .when(message)
                .body();

        balanceHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue submitting request null")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }


    @Test
    public void testGeminiError() throws InterruptedException, GeminiRequestException, IOException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);
        doThrow(new GeminiRequestException("Error with request", 500))
                .when(geminiClient)
                .getBalance(anyString());

        final BalanceHandler balanceHandler = new BalanceHandler(geminiClient);

        doReturn(JsonNodeFactory.instance.objectNode().put("address", "testaddress1").toString())
                .when(message)
                .body();

        balanceHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Gemini API returned an error Error with request")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testBalanceMissingError() throws InterruptedException, GeminiRequestException, IOException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);
        final Message<?> message = Mockito.mock(Message.class);
        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        doReturn(JsonNodeFactory.instance.objectNode().toString())
                .when(response)
                .body();

        doReturn(response)
                .when(geminiClient)
                .getBalance(anyString());

        doReturn(JsonNodeFactory.instance.objectNode().put("address", "testaddress1").toString())
                .when(message)
                .body();

        final BalanceHandler balanceHandler = new BalanceHandler(geminiClient);

        balanceHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Gemini response missing balance")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(503, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testResponseBalanceNotTextualError() throws InterruptedException, GeminiRequestException, IOException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);
        final Message<?> message = Mockito.mock(Message.class);
        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        doReturn(JsonNodeFactory.instance.objectNode().put("balance", 100.0).toString())
                .when(response)
                .body();

        doReturn(response)
                .when(geminiClient)
                .getBalance(anyString());

        doReturn(JsonNodeFactory.instance.objectNode().put("address", "testaddress1").toString())
                .when(message)
                .body();

        final BalanceHandler balanceHandler = new BalanceHandler(geminiClient);

        balanceHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Gemini response balance field is not a string")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(503, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testSuccessfulResponse() throws InterruptedException, GeminiRequestException, IOException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);
        final Message<?> message = Mockito.mock(Message.class);
        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        doReturn(JsonNodeFactory.instance.objectNode().put("balance", "100.0").toString())
                .when(response)
                .body();

        doReturn(response)
                .when(geminiClient)
                .getBalance(anyString());

        doReturn(JsonNodeFactory.instance.objectNode().put("address", "testaddress1").toString())
                .when(message)
                .body();

        final BalanceHandler balanceHandler = new BalanceHandler(geminiClient);

        balanceHandler.consumeMessage(message);

        verify(message).reply(captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("status", "succeeded")
                .set("message", JsonNodeFactory.instance.objectNode()
                        .put("balance", "100.0")).toString(), captor.getValue());
    }
}
