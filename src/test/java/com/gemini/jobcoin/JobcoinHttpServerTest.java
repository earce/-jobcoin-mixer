package com.gemini.jobcoin;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class JobcoinHttpServerTest {

    @Test
    public void testCommandsPayload() {

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        final ObjectNode registerJson = JsonNodeFactory.instance.objectNode()
                .put("payload", " [address 1,address 2 ... address n]")
                .put("description", "Registers n addresses. Returns a deposit address");

        final ObjectNode sendJson = JsonNodeFactory.instance.objectNode()
                .put("payload", "{ \"fromAddress\" : \"srcAddress\", \"toAddress\" : \"destAddress\", \"amount\" : x.xx }")
                .put("description", "Sends [amount] from [srcAddress] to [destAddress] will register with mixer and return associated requestId to track state of mixing request, destAddress must be a depositAddress provided by Jobcoin.");

        final ObjectNode balanceJson = JsonNodeFactory.instance.objectNode()
                .put("description", "Retrieves balance at [balanceAddress]");

        final ObjectNode requestIdJson = JsonNodeFactory.instance.objectNode()
                .put("description", "Retrieves status for [reqId], will return error if requestId" +
                        " does not exist, complete if finished, incomplete if still processing");

        final ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("/v1/register", registerJson);
        node.set("/v1/send", sendJson);
        node.set("/v1/balance?address=<balanceAddress>", balanceJson);
        node.set("/v1/mixingStatus?requestId=<reqId>", requestIdJson);

        final JobcoinHttpServer httpServer = new JobcoinHttpServer();

        final HttpServerResponse response = Mockito.mock(HttpServerResponse.class);
        final RoutingContext ctx = Mockito.mock(RoutingContext.class);

        doReturn(response).when(response).setStatusCode(anyInt());
        doReturn(response).when(response).putHeader(anyString(), anyString());

        doReturn(response).when(ctx).response();

        httpServer.getCommands(ctx);

        verify(response).end(captor.capture());

        Assert.assertEquals(node.toString(), captor.getValue());
    }

    @Test
    public void testStatusUpPayload() {

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        final JobcoinHttpServer httpServer = new JobcoinHttpServer();

        final HttpServerResponse response = Mockito.mock(HttpServerResponse.class);
        final RoutingContext ctx = Mockito.mock(RoutingContext.class);

        doReturn(response).when(response).setStatusCode(anyInt());
        doReturn(response).when(response).putHeader(anyString(), anyString());

        doReturn(response).when(ctx).response();

        httpServer.jobcoinUp(ctx);

        verify(response).end(captor.capture());

        Assert.assertEquals(JsonNodeFactory.instance.objectNode()
                .put("Jobcoin Mixer", "All Systems Operational").toString(), captor.getValue());
    }
}
