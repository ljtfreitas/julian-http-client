package com.github.ljtfreitas.julian.http.client.okhttp;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.HTTPClientRequest;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

class OkHTTPClientRequest implements HTTPClientRequest {

    private final OkHttpClient client;
    private final Request request;

    OkHTTPClientRequest(OkHttpClient client, Request request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public Promise<HTTPClientResponse> execute() {
        CompletableFuture<HTTPClientResponse> responseAsFuture = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                responseAsFuture.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                responseAsFuture.complete(OkHTTPClientResponse.valueOf(response));
            }
        });

        return Promise.pending(responseAsFuture);
    }
}
