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

package com.github.ljtfreitas.julian.http.codec.json.jackson;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.JsonHTTPMessageCodec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON;

public class JacksonJsonHTTPMessageCodec implements JsonHTTPMessageCodec<Object> {

    private static final JacksonJsonHTTPMessageCodec SINGLE_INSTANCE = new JacksonJsonHTTPMessageCodec();

    private final ObjectMapper jsonMapper;
    private final TypeFactory typeFactory;
    private final JsonFactory jsonFactory;

    public JacksonJsonHTTPMessageCodec() {
        this(configure(new ObjectMapper()));
    }

    private static ObjectMapper configure(ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.findAndRegisterModules();
    }

    public JacksonJsonHTTPMessageCodec(ObjectMapper jsonMapper) {
        this.jsonMapper = nonNull(jsonMapper);
        this.typeFactory = jsonMapper.getTypeFactory();
        this.jsonFactory = jsonMapper.getFactory();
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && jsonMapper.canSerialize(javaType.rawClassType());
    }

    @Override
    public HTTPRequestBody write(Object body, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_JSON, () -> serialize(body, encoding));
    }

    private BodyPublisher serialize(Object body, Charset encoding) {
        JsonEncoding jsonEncoding = Stream.of(JsonEncoding.values()).filter(e -> e.getJavaName().equalsIgnoreCase(encoding.name()))
                .findFirst()
                .orElse(JsonEncoding.UTF8);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             JsonGenerator generator = jsonFactory.createGenerator(output, jsonEncoding)) {

            jsonMapper.writeValue(generator, body);

            output.flush();

            return BodyPublishers.ofByteArray(output.toByteArray());
        } catch (IOException e) {
            throw new HTTPRequestWriterException("JSON serialization failed. Source: " + body, e);
        }
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && readable(javaType.get());
    }

    private boolean readable(Type type) {
        return jsonMapper.canDeserialize(typeFactory.constructType(type));
    }

    @Override
    public Optional<CompletableFuture<Object>> read(HTTPResponseBody body, JavaType javaType) {
        return body.readAsInputStream(s -> deserialize(s, javaType));
    }

    private Object deserialize(InputStream bodyAsStream, JavaType javaType) {
        try (InputStreamReader reader = new InputStreamReader(bodyAsStream);
             BufferedReader buffered = new BufferedReader(reader)) {

            return jsonMapper.readValue(buffered, typeFactory.constructType(javaType.get()));
        } catch (IOException e) {
            throw new HTTPResponseReaderException("JSON deserialization failed. The target type was: " + javaType, e);
        }
    }

    public static JacksonJsonHTTPMessageCodec provider() {
        return SINGLE_INSTANCE;
    }
}
