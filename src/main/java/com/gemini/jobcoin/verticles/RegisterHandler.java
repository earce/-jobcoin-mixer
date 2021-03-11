package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.JobcoinHttpServer;
import com.gemini.jobcoin.exception.JsonRequestException;
import com.gemini.jobcoin.external.blockchain.AddressGenerator;
import com.gemini.jobcoin.external.persistence.KVStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gemini.jobcoin.constant.Routes.REGISTER_VERTX_V1;

public class RegisterHandler extends AbstractVerticle {

    private final ObjectMapper mapper = new ObjectMapper();

    private final AddressGenerator addressGenerator;

    private final KVStore<String,List<String>> depositAddressStore;

    public RegisterHandler(final KVStore<String, List<String>> depositAddressStore,
                           final AddressGenerator addressGenerator) {
        this.depositAddressStore = depositAddressStore;
        this.addressGenerator = addressGenerator;
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(REGISTER_VERTX_V1, this::consumeMessage);
    }

    /**
     * Method validates and extracts addresses and then associates them with a
     * freshly generated depositAddress.
     *
     * @param message to process
     */
    private void consumeMessage(final Message<?> message) {
        try {
            final JsonNode msg = mapper.readTree(message.body().toString());
            final List<String> userAddresses = validateAndExtractAddresses(msg);
            final String depositAddress = addressGenerator.generateAddress();

            depositAddressStore.put(depositAddress, userAddresses);

            JobcoinHttpServer.successResponse(message,
                    JsonNodeFactory.instance.objectNode()
                            .put("depositAddress", depositAddress));

        } catch (JsonProcessingException e) {
            JobcoinHttpServer.errorResponse(message, "Issue processing Json", 400);
        } catch (JsonRequestException e) {
            JobcoinHttpServer.errorResponse(message, e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Issue submitting request %s", e.getMessage()), 500);
        }
    }

    /**
     * Extracts validated addresses to register with Jobcoin
     *
     * Validates:
     *  Json is valid array of addresses
     *  Array of addresses is at least 1 in length
     *  Each address is at least 1 character in length
     *  Duplicate addresses are collapsed
     *
     * @param msg to extract addresses from
     * @return list of validated addresses
     */
    public List<String> validateAndExtractAddresses(final JsonNode msg)
            throws JsonRequestException {

        if (!(msg instanceof ArrayNode)) {
            throw new JsonRequestException("Payload is not an array of addresses", 400);
        }
        final ArrayNode addresses = (ArrayNode) msg;
        if (addresses.size() < 1) {
            throw new JsonRequestException("Must register at least 1 address", 422);
        }
        final Set<String> userAddresses = new HashSet<>(addresses.size());
        for (JsonNode n : addresses) {
            if (!n.isTextual()) {
                throw new JsonRequestException(String.format("%s is not a valid address", n.toString()), 400);
            }
            final String address = n.asText();
            if (address.length() < 1) {
                throw new JsonRequestException("Provided an address which is an empty string", 422);
            }
            userAddresses.add(address);
        }
        return new ArrayList<>(userAddresses);
    }
}
