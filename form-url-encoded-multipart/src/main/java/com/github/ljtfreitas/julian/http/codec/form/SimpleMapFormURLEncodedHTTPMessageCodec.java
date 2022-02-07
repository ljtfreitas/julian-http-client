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

package com.github.ljtfreitas.julian.http.codec.form;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.contract.FormSerializer;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.FormURLEncodedHTTPMessageCodec;

import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.Collectors;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_FORM_URLENCODED;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class SimpleMapFormURLEncodedHTTPMessageCodec implements FormURLEncodedHTTPMessageCodec<Map<String, String>> {

    private static final SimpleMapFormURLEncodedHTTPMessageCodec SINGLE_INSTANCE = new SimpleMapFormURLEncodedHTTPMessageCodec();

    private final FormObjectURLEncodedHTTPMessageCodec codec = new FormObjectURLEncodedHTTPMessageCodec();
    private final FormSerializer serializer = new FormSerializer();

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && supportedMap(javaType);
    }

    private boolean supportedMap(JavaType javaType) {
        return javaType.is(Map.class)
                && javaType.parameterized()
                .map(t -> String.class.equals(t.getActualTypeArguments()[0]) && String.class.equals(t.getActualTypeArguments()[1]))
                .orElse(false);
    }

    @Override
    public HTTPRequestBody write(Map<String, String> map, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_FORM_URLENCODED, () -> serialize(map, encoding));
    }

    private Publisher<ByteBuffer> serialize(Map<String, String> map, Charset encoding) {
        return serializer.serialize("", JavaType.parameterized(Map.class, String.class, String.class), map)
                .map(form -> codec.write(form, encoding).serialize())
                .orElseGet(BodyPublishers::noBody);
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && supportedMap(javaType);
    }

    @Override
    public Optional<CompletableFuture<Map<String, String>>> read(HTTPResponseBody body, JavaType javaType) {
        return codec.read(body, javaType)
                .map(future -> future
                        .thenApplyAsync(form -> form.all().entrySet().stream()
                                .flatMap(e -> e.getValue().stream()
                                        .findFirst()
                                        .map(value -> Map.entry(e.getKey(), value)).stream())
                                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    public static SimpleMapFormURLEncodedHTTPMessageCodec provider() {
        return SINGLE_INSTANCE;
    }
}
