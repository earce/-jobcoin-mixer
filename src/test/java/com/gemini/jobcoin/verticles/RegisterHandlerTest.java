package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.external.persistence.InMemoryKVStore;
import com.gemini.jobcoin.external.persistence.KVStore;
import io.vertx.core.eventbus.Message;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class RegisterHandlerTest {

    @Test
    public void testJsonError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, () -> "depositAddress1");

        doReturn("junkjson")
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue processing Json")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testNotArrayError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, () -> "depositAddress1");

        doReturn(JsonNodeFactory.instance.objectNode().toString())
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Payload is not an array of addresses")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testZeroLengthArrayError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, () -> "depositAddress1");

        doReturn(JsonNodeFactory.instance.arrayNode().toString())
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Must register at least 1 address")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testArrayEntryInvalidError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, () -> "depositAddress1");

        doReturn(JsonNodeFactory.instance.arrayNode().add(123).toString())
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "123 is not a valid address")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testEmptyStringAddressError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, () -> "depositAddress1");

        doReturn(JsonNodeFactory.instance.arrayNode().add("").toString())
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Provided an address which is an empty string")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testNullError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, null);

        doReturn(JsonNodeFactory.instance.arrayNode().add("depositAddress1").toString())
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue submitting request null")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }
    @Test
    public void testValidDoubleAddressRegisterSuccess() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        final KVStore<String, List<String>> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final RegisterHandler registerHandler = new RegisterHandler(kvStore, () -> "depositAddress1");

        doReturn(JsonNodeFactory.instance.arrayNode().add("userAddress1").add("userAddress2").toString())
                .when(message)
                .body();

        registerHandler.consumeMessage(message);

        verify(message).reply(captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("status", "succeeded")
                .set("message", JsonNodeFactory.instance.objectNode()
                        .put("depositAddress", "depositAddress1")).toString(), captor.getValue());

        Assert.assertEquals(kvStore.get("depositAddress1").size(), 2);
        Assert.assertEquals(kvStore.get("depositAddress1").get(0), "userAddress1");
        Assert.assertEquals(kvStore.get("depositAddress1").get(1), "userAddress2");
    }

}
