package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.external.persistence.InMemoryKVStore;
import com.gemini.jobcoin.external.persistence.KVStore;
import io.vertx.core.eventbus.Message;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.http.HttpResponse;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class MixingStatusHandlerTest {

    @Test
    public void testJsonError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, Boolean> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final MixingStatusHandler mixingStatusHandler = new MixingStatusHandler(kvStore);

        doReturn("junkjson")
                .when(message)
                .body();

        mixingStatusHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue processing Json")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testRequestIdNotFoundError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final KVStore<String, Boolean> kvStore = new InMemoryKVStore<>();

        final Message<?> message = Mockito.mock(Message.class);
        final MixingStatusHandler mixingStatusHandler = new MixingStatusHandler(kvStore);

        doReturn(JsonNodeFactory.instance.objectNode().put("requestId", "ID123").toString())
                .when(message)
                .body();

        mixingStatusHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Request Id=[ID123] is not recognized by Jobcoin")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(422, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testNullError() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final MixingStatusHandler mixingStatusHandler = new MixingStatusHandler(null);

        doReturn(JsonNodeFactory.instance.objectNode().put("requestId", "ID123").toString())
                .when(message)
                .body();

        mixingStatusHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Issue submitting request null")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(500, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testRequestIdMissingError()  {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        final Message<?> message = Mockito.mock(Message.class);
        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        doReturn(JsonNodeFactory.instance.objectNode().toString())
                .when(message)
                .body();

        final KVStore<String, Boolean> kvStore = new InMemoryKVStore<>();

        final MixingStatusHandler mixingStatusHandler = new MixingStatusHandler(kvStore);

        mixingStatusHandler.consumeMessage(message);

        verify(message).fail(statusCodeCaptor.capture(), captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("message", "Payload missing [requestId]")
                .put("status", "failed").toString(), captor.getValue());
        Assert.assertEquals(400, (int) statusCodeCaptor.getValue());
    }

    @Test
    public void testSuccessfulResponse() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        final Message<?> message = Mockito.mock(Message.class);
        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        final KVStore<String, Boolean> kvStore = new InMemoryKVStore<>();
        kvStore.put("ID123", true);

        doReturn(JsonNodeFactory.instance.objectNode().put("requestId", "ID123").toString())
                .when(message)
                .body();

        final MixingStatusHandler mixingStatusHandler = new MixingStatusHandler(kvStore);

        mixingStatusHandler.consumeMessage(message);

        verify(message).reply(captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("status", "succeeded")
                .set("message", JsonNodeFactory.instance.objectNode()
                        .put("status", "complete")).toString(), captor.getValue());
    }
}
