package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gemini.jobcoin.JobcoinHttpServer;
import com.gemini.jobcoin.exception.GeminiRequestException;
import com.gemini.jobcoin.exception.JsonRequestException;
import com.gemini.jobcoin.external.http.GeminiClient;
import com.gemini.jobcoin.external.persistence.KVStore;
import com.gemini.jobcoin.helper.Validator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

import java.util.List;


import static com.gemini.jobcoin.constant.Routes.MIXER_VERTX_V1;
import static com.gemini.jobcoin.constant.Routes.SEND_VERTX_V1;
import static com.gemini.jobcoin.external.blockchain.JobcoinAddressGenerator.JOBCOIN_HOUSE_ADDRESS;

public class SendHandler extends AbstractVerticle {

    private final ObjectMapper mapper = new ObjectMapper();

    private final GeminiClient geminiClient;

    private final KVStore<String, List<String>> depositAddressStore;

    public SendHandler(final GeminiClient geminiClient,
                       final KVStore<String,List<String>> depositAddressStore) {
        this.geminiClient = geminiClient;
        this.depositAddressStore = depositAddressStore;
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(SEND_VERTX_V1, this::consumeMessage);
    }

    /**
     * Method constructs url to send request to for transferring Jobcoins
     *
     * @param message to process
     */
    void consumeMessage(final Message<?> message) {
        try {
            final JsonNode msg = mapper.readTree(message.body().toString());
            final MixingRequest mixingRequest = buildRequest(msg);

            // transfer from source address to deposit address
            geminiClient.transferAmount(
                    mixingRequest.sourceAddress, mixingRequest.depositAddress, mixingRequest.amount);

            // from deposit address to house address
            geminiClient.transferAmount(
                    mixingRequest.depositAddress, JOBCOIN_HOUSE_ADDRESS, mixingRequest.amount);

            registerToMixingEngine(message, mixingRequest);

        } catch (GeminiRequestException e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Gemini API returned an error %s", e.getMessage()), e.getStatusCode());
        } catch (JsonRequestException e) {
            JobcoinHttpServer.errorResponse(message, e.getMessage(), e.getStatusCode());
        } catch (JsonProcessingException e) {
            JobcoinHttpServer.errorResponse(message, "Issue processing Json", 400);
        } catch (Exception e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Issue submitting request %s", e.getMessage()), 500);
        }
    }

    /**
     * Registers request to mixing engine which will return an id that
     * can be used to track state of mixing
     *
     * @param message to respond to
     * @param mixingRequest we are sending to engine
     */
    private void registerToMixingEngine(final Message<?> message, final MixingRequest mixingRequest) {
        vertx.eventBus().request(MIXER_VERTX_V1, mixingRequest.toString(), event -> {
            if (event.succeeded()) {
                try {
                    final ObjectNode msg = (ObjectNode) mapper.readTree((String)event.result().body());
                    JobcoinHttpServer.successResponse(message, msg.get("message").toString());
                } catch (Exception e) {
                    JobcoinHttpServer.errorResponse(message, String.format(
                            "Issue processing mixing engine response %s", event.result().body()), 500);
                }
            } else {
                try {
                    final ObjectNode msg = (ObjectNode) mapper.readTree(event.cause().getMessage());
                    JobcoinHttpServer.errorResponse(message, msg.get("message").asText(), 500);
                } catch (Exception e) {
                    JobcoinHttpServer.errorResponse(message, String.format(
                            "Issue processing mixing engine response %s", event.cause().getMessage()), 500);
                }

            }
        });
    }

    /**
     * Validation of request for registering addresses with Jobcoin
     *
     * Validates:
     *  Json is valid with all necessary fields
     *  Address we are sending to is registered with Jobcoins
     *      Simply existing with a balance on jobcoin.gemini.com is not
     *      indicative of this because the destination address needs to
     *      be associated with a set of addresses where the mixer will
     *      ultimately send all the money to.
     *  Each address is at least 1 character in length
     *
     * @param msg extract fields and build the mixing request with
     * @return request used to send to mixing engine
     **/
    private MixingRequest buildRequest(final JsonNode msg) throws JsonRequestException {

        if (!(msg instanceof ObjectNode)) {
            throw new JsonRequestException("Payload is not json object", 400);
        }
        final String fromAddr = Validator.stringField("fromAddress", msg);
        final String toAddr = Validator.stringField("toAddress", msg);

        if (!depositAddressStore.containsKey(toAddr)) {
            throw new JsonRequestException(String.format("[%s] is not an address registered to Jobcoin", toAddr), 422);
        }
        if (!msg.has("amount")) {
            throw new JsonRequestException("Payload missing amount", 400);
        }
        if (!msg.get("amount").isNumber()) {
            throw new JsonRequestException("Amount needs to be a number", 400);
        }
        final String amount = msg.get("amount").toString();
        if (Double.compare(msg.get("amount").doubleValue(), 0.0) <= 0) {
            throw new JsonRequestException("Amount needs to be greater then 0.0", 400);
        }

        return new MixingRequest(
                fromAddr,
                toAddr,
                amount
        );
    }

    static class MixingRequest {

        public final String sourceAddress;

        public final String depositAddress;

        public final String amount;

        public MixingRequest(final String sourceAddress,
                             final String depositAddress,
                             final String amount) {
            this.sourceAddress = sourceAddress;
            this.depositAddress = depositAddress;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return JsonNodeFactory.instance.objectNode()
                    .put("sourceAddress", sourceAddress)
                    .put("depositAddress", depositAddress)
                    .put("amount", amount)
                    .toString();
        }
    }
}
