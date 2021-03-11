package com.gemini.jobcoin.external.http;

import com.gemini.jobcoin.exception.GeminiRequestException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class GeminiClientTest {

    @Test
    public void testErrorCodeBalance() throws IOException, InterruptedException {

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        ArgumentCaptor< HttpResponse.BodyHandler<String>> captor1 = ArgumentCaptor.forClass(HttpResponse.BodyHandler.class);

        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(400);

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        doReturn(response).when(httpClient).send(any(), any());

        boolean threwError = false;
        try {
            final GeminiClient geminiClient = new GeminiClient(httpClient);
            geminiClient.getBalance("testaddress");
        } catch (Exception e) {
            threwError = true;
        }
        Assert.assertTrue(threwError);

        verify(httpClient).send(captor.capture(), captor1.capture());

        Assert.assertEquals("http://jobcoin.gemini.com/cultivate-duvet/api/addresses/testaddress",
                captor.getValue().uri().toString());
    }

    @Test
    public void testErrorCodeTransfer() throws IOException, InterruptedException {

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        ArgumentCaptor< HttpResponse.BodyHandler<String>> captor1 = ArgumentCaptor.forClass(HttpResponse.BodyHandler.class);

        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(400);

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        doReturn(response).when(httpClient).send(any(), any());

        boolean threwError = false;
        try {
            final GeminiClient geminiClient = new GeminiClient(httpClient);
            geminiClient.transferAmount("fromerick", "tomark", "100");
        } catch (Exception e) {
            threwError = true;
        }
        Assert.assertTrue(threwError);

        verify(httpClient).send(captor.capture(), captor1.capture());

        Assert.assertEquals("http://jobcoin.gemini.com/cultivate-duvet/api/transactions?fromAddress=fromerick&toAddress=tomark&amount=100",
                captor.getValue().uri().toString());
    }

    @Test
    public void testBalance() throws IOException, InterruptedException, GeminiRequestException {

        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        doReturn(response).when(httpClient).send(any(), any());

        final GeminiClient geminiClient = new GeminiClient(httpClient);
        Assert.assertNotNull(geminiClient.getBalance("fromerick"));
    }

    @Test
    public void testTransfer() throws IOException, InterruptedException, GeminiRequestException {

        final HttpResponse<String> response = (HttpResponse<String>)Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        doReturn(response).when(httpClient).send(any(), any());

        final GeminiClient geminiClient = new GeminiClient(httpClient);
        Assert.assertNotNull(geminiClient.transferAmount("fromerick", "tomark", "100"));
    }
}
