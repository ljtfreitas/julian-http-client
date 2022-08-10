package com.github.ljtfreitas.julian.http.client.okhttp;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

class OkHTTPRequestBody extends RequestBody {

    private final MediaType mediaType;
    private final HTTPRequestBody source;

    OkHTTPRequestBody(MediaType mediaType, HTTPRequestBody source) {
        this.mediaType = mediaType;
        this.source = source;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public void writeTo(BufferedSink bufferedSink) {
        source.serialize().subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(ByteBuffer item) {
                Attempt.just(() -> bufferedSink.write(item)).prop();
            }

            @Override
            public void onError(Throwable throwable) {
                throw new IllegalStateException(throwable);
            }

            @Override
            public void onComplete() {
                Attempt.just(bufferedSink::flush).prop();
            }
        });
    }
}
