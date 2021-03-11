package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.exception.GeminiRequestException;
import com.gemini.jobcoin.external.http.GeminiClient;
import com.gemini.jobcoin.external.persistence.InMemoryKVStore;
import com.gemini.jobcoin.external.persistence.KVStore;
import io.vertx.core.eventbus.Message;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class MixingEngineTest {

    @Test
    public void testInboundMixingRequest() throws InterruptedException, IOException, GeminiRequestException {

        final KVStore<String, List<String>> depositAddressStore = new InMemoryKVStore<>();
        depositAddressStore.put("TO456", Arrays.asList("USERADDRESS1", "USERADDRESS2"));

        final KVStore<String, Boolean> requestStore = new InMemoryKVStore<>();
        final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);

        final MixingEngine engine = new MixingEngine(depositAddressStore, requestStore, () -> "REQUEST123",
            geminiClient, 50, 10, 15, 14);

        final Message<?> message = Mockito.mock(Message.class);

        doReturn(JsonNodeFactory.instance.objectNode()
                .put("depositAddress", "TO456")
                .put("amount", "100").toString())
                .when(message)
                .body();

        engine.consumeMessage(message);

        while (!requestStore.containsKey("REQUEST123")) {
            Thread.sleep(10);
        }

        while (!requestStore.get("REQUEST123")) {
            Thread.sleep(10);
        }

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(geminiClient, atLeastOnce()).transferAmount(anyString(), anyString(), captor.capture());

        Assert.assertEquals(captor.getAllValues().size(), 14);
        final BigDecimal bd = captor.getAllValues().stream().map(BigDecimal::new).reduce(BigDecimal::add).get();
        Assert.assertEquals(0.0, bd.compareTo(new BigDecimal("100")), 0.0);

    }
}
