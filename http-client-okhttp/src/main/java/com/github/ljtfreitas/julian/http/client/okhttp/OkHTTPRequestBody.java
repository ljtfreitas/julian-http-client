/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
