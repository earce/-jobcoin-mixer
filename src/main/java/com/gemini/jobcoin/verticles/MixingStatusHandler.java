package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.JobcoinHttpServer;
import com.gemini.jobcoin.exception.JobcoinException;
import com.gemini.jobcoin.exception.JsonRequestException;
import com.gemini.jobcoin.external.persistence.KVStore;
import com.gemini.jobcoin.helper.Validator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

import static com.gemini.jobcoin.constant.Routes.MIXING_STATUS_VERTX_V1;

public class MixingStatusHandler extends AbstractVerticle {

    private final ObjectMapper mapper = new ObjectMapper();

    final KVStore<String,Boolean> requestStore;

    public MixingStatusHandler(final KVStore<String,Boolean> requestStore) {
        this.requestStore = requestStore;
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(MIXING_STATUS_VERTX_V1, this::consumeMessage);
    }

    /**
     * Handler to request the balance for any account. Makes a request to the Gemini
     * API and returns the result back to the caller.
     *
     * @param message to process
     */
    private void consumeMessage(final Message<?> message) {
        try {
            final JsonNode msg = mapper.readTree(message.body().toString());
            final String requestId = Validator.stringField("requestId", msg);

            if (!requestStore.containsKey(requestId)) {
                throw new JobcoinException(
                        String.format("Request Id=[%s] is not recognized by Jobcoin", requestId), 422);
            }
            JobcoinHttpServer.successResponse(message,
                    JsonNodeFactory.instance.objectNode()
                            .put("status", requestStore.get(requestId) ? "complete" : "incomplete"));

        } catch (JsonProcessingException e) {
            JobcoinHttpServer.errorResponse(message, "Issue processing Json", 400);
        } catch (JsonRequestException e) {
            JobcoinHttpServer.errorResponse(message, e.getMessage(), e.getStatusCode());
        } catch (JobcoinException e) {
            JobcoinHttpServer.errorResponse(message, e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Issue processing %s", e.getMessage()), 500);
        }
    }
}
