package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.exception.GeminiRequestException;
import com.gemini.jobcoin.external.http.GeminiClient;
import com.gemini.jobcoin.external.persistence.InMemoryKVStore;
import com.gemini.jobcoin.external.persistence.KVStore;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class SendHandlerTest {

    @Test
    public void testJsonError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, null);

        doReturn("junkjson")
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

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
        final SendHandler sendHandler = new SendHandler(null, null);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456").toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue submitting request null")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testFromAddressNotFoundError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, null);

        doReturn(JsonNodeFactory.instance.objectNode().toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Payload missing [fromAddress]")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testToAddressNotFoundError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, null);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123").toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Payload missing [toAddress]")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testNotObjectNodeError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, null);

        doReturn(JsonNodeFactory.instance.arrayNode().toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Payload is not json object")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }


    @Test
    public void testAddressNotRegisteredError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, new InMemoryKVStore<>());

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456").toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "[TO456] is not an address registered to Jobcoin")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(422, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testAmountMissingError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();
        kvStore.put("TO456", new LinkedList<>());
        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, kvStore);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456").toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Payload missing amount")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testAmountNotNumericalError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();
        kvStore.put("TO456", new LinkedList<>());
        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, kvStore);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456")
                .put("amount", "HI").toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Amount needs to be a number")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testAmountZeroError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();
        kvStore.put("TO456", new LinkedList<>());
        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, kvStore);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456")
                .put("amount", 0.0).toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Amount needs to be greater then 0.0")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }


    @Test
    public void testAmountNegError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();
        kvStore.put("TO456", new LinkedList<>());
        final Message<?> message = Mockito.mock(Message.class);
        final SendHandler sendHandler = new SendHandler(null, kvStore);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456")
                .put("amount", -1.0).toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Amount needs to be greater then 0.0")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testGeminiError() throws InterruptedException, GeminiRequestException, IOException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);

        doThrow(new GeminiRequestException("Error with request", 500))
                .when(geminiClient)
                .transferAmount(anyString(), anyString(), anyString());

        final KVStore<String, List<String>> depositAddressStore = new InMemoryKVStore<>();
        depositAddressStore.put("TO456", Arrays.asList("USERADDRESS1", "USERADDRESS2"));

        final SendHandler sendHandler = new SendHandler(geminiClient, depositAddressStore);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456")
                .put("amount", 1.0).toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Gemini API returned an error Error with request")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testSuccessfulMixingEngineResponse() throws InterruptedException {
        // deploy vertx
        final Vertx vertx = Vertx.vertx();

        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);

        final KVStore<String, List<String>> depositAddressStore = new InMemoryKVStore<>();
        depositAddressStore.put("TO456", Arrays.asList("USERADDRESS1", "USERADDRESS2"));

        final KVStore<String,Boolean> requestStore = new InMemoryKVStore<>();

        final SendHandler sendHandler = new SendHandler(geminiClient, depositAddressStore);

        vertx.deployVerticle(new MixingEngine(depositAddressStore, requestStore, () -> "REQUEST123", geminiClient, 1000, 500, 2, 1));
        vertx.deployVerticle(sendHandler);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        final Message<?> message = Mockito.mock(Message.class);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456")
                .put("amount", 1.0).toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        // must wait until request is added to queue and set to true (processed)
        while (!requestStore.containsKey("REQUEST123")) {
            Thread.sleep(10);
        }

        while (!requestStore.get("REQUEST123")) {
            Thread.sleep(10);
        }

        verify(message).reply(captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("status", "succeeded")
                .put("message", JsonNodeFactory.instance.objectNode()
                .put("requestId", "REQUEST123").toString())
                .toString(), captor.getValue());
    }

    @Test
    public void testFailedMixingEngineResponse() throws InterruptedException {
        // deploy vertx
        final Vertx vertx = Vertx.vertx();

        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);

        final KVStore<String, List<String>> depositAddressStore = new InMemoryKVStore<>();
        depositAddressStore.put("TO456", Arrays.asList("USERADDRESS1", "USERADDRESS2"));

        final KVStore<String,Boolean> requestStore = new InMemoryKVStore<>();

        final SendHandler sendHandler = new SendHandler(geminiClient, depositAddressStore);

        vertx.deployVerticle(new MixingEngine(null, requestStore, () -> "REQUEST123", geminiClient,
                1000, 500, 2, 1));
        vertx.deployVerticle(sendHandler);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("fromAddress", "FROM123")
                .put("toAddress", "TO456")
                .put("amount", 1.0).toString())
                .when(message)
                .body();

        sendHandler.consumeMessage(message);

        Thread.sleep(2000);
        // not a great way to guarantee processing has finished, the right way would be to add a callback
        // and ensure the method has returned

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue submitting request null")
                .put("status", "failed")
                .toString(), captor.getValue());

        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }
}
