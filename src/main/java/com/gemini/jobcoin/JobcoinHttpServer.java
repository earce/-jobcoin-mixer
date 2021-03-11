package com.gemini.jobcoin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.gemini.jobcoin.constant.Routes.BALANCE_V1;
import static com.gemini.jobcoin.constant.Routes.COMMANDS_V1;
import static com.gemini.jobcoin.constant.Routes.MIXING_STATUS_V1;
import static com.gemini.jobcoin.constant.Routes.MIXING_STATUS_VERTX_V1;
import static com.gemini.jobcoin.constant.Routes.REGISTER_V1;
import static com.gemini.jobcoin.constant.Routes.SEND_V1;

import static com.gemini.jobcoin.constant.Routes.BALANCE_VERTX_V1;
import static com.gemini.jobcoin.constant.Routes.REGISTER_VERTX_V1;
import static com.gemini.jobcoin.constant.Routes.SEND_VERTX_V1;
import static com.gemini.jobcoin.constant.Routes.STATUS;
import static com.gemini.jobcoin.constant.Web.APPLICATION_JSON;
import static com.gemini.jobcoin.constant.Web.CONTENT_TYPE;


public class JobcoinHttpServer extends AbstractVerticle {

    @Override
    public void start() {

        final HttpServer httpServer = vertx.createHttpServer();

        final Router router = Router.router(vertx);

        router.route(HttpMethod.GET, STATUS).handler(this::jobcoinUp);
        router.route(HttpMethod.GET, COMMANDS_V1).handler(this::getCommands);
        router.route(HttpMethod.GET, BALANCE_V1).handler(ctx -> getHandler(ctx, BALANCE_VERTX_V1));
        router.route(HttpMethod.GET, MIXING_STATUS_V1).handler(ctx -> getHandler(ctx, MIXING_STATUS_VERTX_V1));
        router.route(HttpMethod.POST, REGISTER_V1).handler(ctx -> postHandler(ctx, REGISTER_VERTX_V1));
        router.route(HttpMethod.POST, SEND_V1).handler(ctx -> postHandler(ctx, SEND_VERTX_V1));

        httpServer
                .requestHandler(router)
                .listen(8111);
    }

    public static void successResponse(final Message<?> message,
                                       final String msg) {
        final ObjectNode response = JsonNodeFactory.instance.objectNode()
                .put("status", "succeeded")
                .put("message", msg);
        message.reply(response.toString());
    }

    public static void successResponse(final Message<?> message,
                                       final JsonNode msg) {
        final ObjectNode response = JsonNodeFactory.instance.objectNode()
                .put("status", "succeeded")
                .set("message", msg);
        message.reply(response.toString());
    }

    public static void errorResponse(final Message<?> message,
                                     final String errorMsg,
                                     int errorCode) {
        final ObjectNode response = JsonNodeFactory.instance.objectNode()
                .put("message", errorMsg)
                .put("status", "failed");
        message.fail(errorCode, response.toString());
    }

    /**
     * Generic POST handler manages logic required for publishing messages onto the
     * event bus and getting the response asynchronously.
     *
     * @param ctx for request
     * @param route on the event bus to send message to
     */
    private void postHandler(final RoutingContext ctx, final String route) {
        ctx.request().bodyHandler(handler -> {
            final String body = new String(handler.getBytes(), StandardCharsets.UTF_8);
            vertx.eventBus().request(route, body, event -> {
                if (event.succeeded()) {
                    ctx.response()
                            .setStatusCode(200)
                            .end(event.result().body().toString());
                } else {
                    ctx.response()
                            .setStatusCode(((ReplyException)event.cause()).failureCode())
                            .end(event.cause().getMessage());
                }
            });
        });
    }

    /**
     * Generic GET handler manages logic required for publishing messages onto the
     * event bus and getting the response asynchronously.
     *
     * @param ctx for request
     * @param route on the event bus to send message to
     */
    private void getHandler(final RoutingContext ctx, final String route) {
        final ObjectNode params = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String,String> e : ctx.request().params()) {
            params.put(e.getKey(), e.getValue());
        }
        ctx.request().bodyHandler(handler -> {
            final String s = params.toString();
            vertx.eventBus().request(route, s, event -> {
                if (event.succeeded()) {
                    ctx.response()
                            .setStatusCode(200)
                            .end(event.result().body().toString());
                } else {
                    ctx.response()
                            .setStatusCode(((ReplyException)event.cause()).failureCode())
                            .end(event.cause().getMessage());
                }
            });
        });
    }
    /**
     * Returns simple json payload with http code showing Jobcoin app is operational.
     *
     * Meant to be expanded to support expansive health checks for Jobcoin. For
     * purposes of this demo just used to ping app and make sure API is usable before
     * firing subsequent requests.
     *
     * @param ctx for request
     */
    void jobcoinUp(final RoutingContext ctx) {
        ctx.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(200)
                .end(
                        JsonNodeFactory.instance.objectNode()
                        .put("Jobcoin Mixer", "All Systems Operational").toString()
                );
    }

    /**
     * Exposes endpoint to request all available commands for Jobcoin API
     *
     * @param ctx for request
     */
    void getCommands(final RoutingContext ctx) {

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

        ctx.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(200)
                .end(node.toString());
    }
}
