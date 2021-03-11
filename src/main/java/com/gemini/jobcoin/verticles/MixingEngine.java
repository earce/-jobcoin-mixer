package com.gemini.jobcoin.verticles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.gemini.jobcoin.JobcoinHttpServer;
import com.gemini.jobcoin.external.http.GeminiClient;
import com.gemini.jobcoin.external.persistence.KVStore;
import com.gemini.jobcoin.external.persistence.UUIDGenerator;
import com.gemini.jobcoin.helper.JobcoinMath;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.gemini.jobcoin.constant.Routes.MIXER_VERTX_V1;
import static com.gemini.jobcoin.external.blockchain.JobcoinAddressGenerator.JOBCOIN_HOUSE_ADDRESS;

public class MixingEngine extends AbstractVerticle {

    private final static Logger logger = LoggerFactory.getLogger(MixingEngine.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final SecureRandom ran = new SecureRandom();

    private static final ScheduledExecutorService executorService =
            Executors.newScheduledThreadPool(2);

    private final int maxIntervalMs;

    private final int minIntervalMs;

    private final int maxParts;

    private final int minParts;

    private final BlockingQueue<MixingTask> queue = new LinkedBlockingQueue<>();

    private final UUIDGenerator requestIdGenerator;

    private final KVStore<String,List<String>> depositAddressStore;

    private final KVStore<String,Boolean> requestStore;

    private final GeminiClient geminiClient;

    public MixingEngine(final KVStore<String, List<String>> depositAddressStore,
                        final KVStore<String, Boolean> requestStore,
                        final UUIDGenerator requestIdGenerator,
                        final GeminiClient geminiClient) {
        this(
                depositAddressStore,
                requestStore,
                requestIdGenerator,
                geminiClient,
                20000,
                1000,
                3,
                10);
    }

    public MixingEngine(final KVStore<String, List<String>> depositAddressStore,
                        final KVStore<String, Boolean> requestStore,
                        final UUIDGenerator requestIdGenerator,
                        final GeminiClient geminiClient,
                        int maxIntervalMs,
                        int minIntervalMs,
                        int maxParts,
                        int minParts) {
        this.depositAddressStore = depositAddressStore;
        this.requestStore = requestStore;
        this.requestIdGenerator = requestIdGenerator;
        this.geminiClient = geminiClient;
        this.maxIntervalMs = maxIntervalMs;
        this.minIntervalMs = minIntervalMs;
        this.maxParts = maxParts;
        this.minParts = minParts;
        new Thread(this::mixerScheduler).start();
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(MIXER_VERTX_V1, this::consumeMessage);
    }

    /**
     * Consumes a request to start mixing an amount from the associated deposit address.
     *
     * Method creates a unique id for the request and schedules the mixing task onto
     * the queue to process.
     *
     * The unique id is then returned to the caller to have a handle on the request's
     * mixing lifecycle.
     *
     * @param message to respond to
     */
    void consumeMessage(final Message<?> message) {
        try {
            final String requestId = requestIdGenerator.generateId();

            final JsonNode mixingRequest = mapper.readTree(message.body().toString());

            final String amount = mixingRequest.get("amount").textValue();
            final int partCount = ran.nextInt(maxParts - minParts) + minParts;

            final String depositAddress = mixingRequest.get("depositAddress").asText();

            final LinkedList<BigDecimal> quantities = JobcoinMath.breakUpDecimalIntoDecimals(amount, partCount);
            final List<String> userOwnedAddresses = depositAddressStore.get(depositAddress);

            logger.info(String.format("Request Id=[%s] Deposit Address=[%s] Amount=[%s] No Of Parts=[%d] Quantities=%s",
                    requestId, depositAddress, amount, quantities.size(), quantities.toString()));


            requestStore.put(requestId, false);

            queue.add(new MixingTask(
                    quantities,
                    userOwnedAddresses,
                    requestId,
                    this));


            JobcoinHttpServer.successResponse(message, JsonNodeFactory.instance.objectNode()
                    .put("requestId", requestId));
        } catch (Exception e) {
            JobcoinHttpServer.errorResponse(message,
                    String.format("Issue submitting request %s", e.getMessage()), 500);
        }
    }

    /**
     * Runnable is dedicating to scheduling mixing tasks on a random basis to make
     * attacks like timing correlation more difficult to achieve.
     *
     * Uses a blocking queue to wait until mixing tasks become available to process
     * to avoid churning the CPU needlessly.
     *
     * @return Runnable which has capability of scheduling for random time
     */
    private Runnable mixerScheduler() {
        while (true) {
            try {
                final MixingTask mixingTask = queue.take();
                int delay = ran.nextInt(maxIntervalMs - minIntervalMs) + minIntervalMs;
                executorService.schedule(mixingTask, delay, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * MixingTask represents the work involved in sending the broken up amount to any
     * combination of the userOwnedAddresses.
     *
     * Note:
     *      If we get an error back from the Gemini API, we are assuming failure. We
     *      do not remove the quantity we tried to process from the list and attempt
     *      to send it again later.
     *
     *      This behavior may not be desirable depending on the constraints of the
     *      application. There are risks to submitting a transaction again if Gemini
     *      reported a failure or the request timed out but in-fact succeeded. Building
     *      guarantees to protect against this are a larger endeavor but worth mentioning.
     */
    public static class MixingTask implements Runnable {

        private final LinkedList<BigDecimal> quantities;
        private final List<String> userOwnedAddresses;
        private final String requestId;
        private final MixingEngine mixingEngine;

        public MixingTask(final LinkedList<BigDecimal> quantities,
                          final List<String> userOwnedAddresses,
                          final String requestId,
                          final MixingEngine mixingEngine) {
            this.quantities = quantities;
            this.userOwnedAddresses = userOwnedAddresses;
            this.requestId = requestId;
            this.mixingEngine = mixingEngine;
        }


        @Override
        public void run() {
            try {
                if (quantities.isEmpty()) {
                    return;
                }

                final BigDecimal quantity = quantities.peek();   // defensively peek the list

                int addressIndexToSend = ran.nextInt(userOwnedAddresses.size());
                final String addressTo = userOwnedAddresses.get(addressIndexToSend);

                logger.info(String.format("Sending=[%s] to User Address=[%s] from Jobcoin House Address",
                        quantity, addressTo));

                mixingEngine.geminiClient.transferAmount(
                        JOBCOIN_HOUSE_ADDRESS, addressTo, quantity.toPlainString());

                quantities.poll(); // we only remove the quantity permanently if Gemini returns a 200

                if (quantities.isEmpty()) {
                    logger.info(String.format("Request Id=[%s] has completed mixing", requestId));
                    mixingEngine.requestStore.put(requestId, true);  // mark request id as completed
                } else {
                    mixingEngine.queue.add(this);                    // if anything left add task back to queue
                }

            } catch (Exception e) {
                mixingEngine.queue.add(this); // if an exception is throw we should reschedule this to run
                e.printStackTrace();
            }
        }
    }
}
