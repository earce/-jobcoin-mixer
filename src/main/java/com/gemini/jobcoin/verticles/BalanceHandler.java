package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.JobcoinHttpServer;
import com.gemini.jobcoin.exception.GeminiRequestException;
import com.gemini.jobcoin.exception.JsonRequestException;
import com.gemini.jobcoin.external.http.GeminiClient;
import com.gemini.jobcoin.helper.Validator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

import java.net.http.HttpResponse;

import static com.gemini.jobcoin.constant.Routes.BALANCE_VERTX_V1;

public class BalanceHandler extends AbstractVerticle {

    private final ObjectMapper mapper = new ObjectMapper();

    private final GeminiClient geminiClient;

    public BalanceHandler(final GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(BALANCE_VERTX_V1, this::consumeMessage);
    }

    /**
     * Handler to request the balance for any account. Makes a request to the Gemini
     * API and returns the result back to the caller.
     *
     * @param message to process
     */
    void consumeMessage(final Message<?> message) {
        try {
            final JsonNode msg = mapper.readTree(message.body().toString());
            final String address = Validator.stringField("address", msg);

            final HttpResponse<String> response = geminiClient.getBalance(address);

            final JsonNode geminiMsg = mapper.readTree(response.body());
            if (!geminiMsg.has("balance")) {
                throw new JsonRequestException("Gemini response missing balance", 503);
            }
            if (!geminiMsg.get("balance").isTextual()) {
                throw new JsonRequestException("Gemini response balance field is not a string", 503);
            }
            JobcoinHttpServer.successResponse(message,
                    JsonNodeFactory.instance.objectNode()
                            .set("balance", geminiMsg.get("balance")));

        } catch (JsonProcessingException e) {
            JobcoinHttpServer.errorResponse(message, "Issue processing Json", 400);
        } catch (JsonRequestException e) {
            JobcoinHttpServer.errorResponse(message, e.getMessage(), e.getStatusCode());
        } catch (GeminiRequestException e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Gemini API returned an error %s", e.getMessage()), e.getStatusCode());
        } catch (Exception e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Issue submitting request %s", e.getMessage()), 500);
        }
    }
}
