package com.gemini.jobcoin.external.http;

import com.gemini.jobcoin.exception.GeminiRequestException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.gemini.jobcoin.constant.Web.APPLICATION_JSON;
import static com.gemini.jobcoin.constant.Web.CONTENT_TYPE;

public class GeminiClient {

    private final HttpClient httpClient;

    public GeminiClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public HttpResponse<String> getBalance(final String address)
            throws IOException, InterruptedException, GeminiRequestException {

        final String url = String.format(
                "http://jobcoin.gemini.com/cultivate-duvet/api/addresses/%s", address);

        final HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header(CONTENT_TYPE, APPLICATION_JSON)
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new GeminiRequestException(response.body(), response.statusCode());
        }
        return response;
    }

    public HttpResponse<String> transferAmount(final String fromAddress,
                                               final String toAddress,
                                               final String amount)
            throws IOException, InterruptedException, GeminiRequestException {

         final String url = String.format(
                    "http://jobcoin.gemini.com/cultivate-duvet/api/transactions?fromAddress=%s&toAddress=%s&amount=%s",
                    fromAddress, toAddress, amount);

        final HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .header(CONTENT_TYPE, APPLICATION_JSON)
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new GeminiRequestException(response.body(), response.statusCode());
        }
        return response;
    }
}
