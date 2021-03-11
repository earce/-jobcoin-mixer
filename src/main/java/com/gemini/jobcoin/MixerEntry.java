package com.gemini.jobcoin;

import com.gemini.jobcoin.external.blockchain.AddressGenerator;
import com.gemini.jobcoin.external.blockchain.JobcoinAddressGenerator;
import com.gemini.jobcoin.external.http.GeminiClient;
import com.gemini.jobcoin.external.persistence.InMemoryKVStore;
import com.gemini.jobcoin.external.persistence.KVStore;
import com.gemini.jobcoin.external.persistence.RequestIdGenerator;
import com.gemini.jobcoin.external.persistence.UUIDGenerator;
import com.gemini.jobcoin.verticles.BalanceHandler;
import com.gemini.jobcoin.verticles.MixingEngine;
import com.gemini.jobcoin.verticles.MixingStatusHandler;
import com.gemini.jobcoin.verticles.RegisterHandler;
import com.gemini.jobcoin.verticles.SendHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

public class MixerEntry {

    private final static Logger logger = LoggerFactory.getLogger(MixerEntry.class);

    public static void main(String[] args) {

        final Vertx vertx = Vertx.vertx();

        final GeminiClient geminiClient = new GeminiClient(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());

        // define your worker config based on hardware etc
        final DeploymentOptions workerOpts = new DeploymentOptions()
                .setWorker(true)
                .setWorkerPoolSize(2)
                .setInstances(2);

        final KVStore<String, List<String>> depositAddressStore = new InMemoryKVStore<>();
        final KVStore<String,Boolean> requestStore = new InMemoryKVStore<>();
        final UUIDGenerator requestIdGenerator = new RequestIdGenerator();
        final AddressGenerator addressGenerator = new JobcoinAddressGenerator();

        vertx.deployVerticle(new JobcoinHttpServer());

        vertx.deployVerticle(() -> new RegisterHandler(depositAddressStore, addressGenerator), workerOpts);
        vertx.deployVerticle(() -> new SendHandler(geminiClient, depositAddressStore), workerOpts);
        vertx.deployVerticle(() -> new BalanceHandler(geminiClient), workerOpts);
        vertx.deployVerticle(() -> new MixingStatusHandler(requestStore), workerOpts);

        vertx.deployVerticle(() -> new MixingEngine(
                depositAddressStore, requestStore, requestIdGenerator, geminiClient), workerOpts);

        logger.info("Jobcoin mixer is up");
    }
}
