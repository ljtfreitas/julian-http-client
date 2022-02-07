/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.github.ljtfreitas.julian.http.codec.json.jsonp;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.JsonHTTPMessageCodec;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON;
import static java.util.Collections.emptyMap;

public class JsonPHTTPMessageCodec implements JsonHTTPMessageCodec<JsonStructure> {

    private static final JsonPHTTPMessageCodec SINGLE_INSTANCE = new JsonPHTTPMessageCodec();

    private final JsonReaderFactory jsonReaderFactory;
    private final JsonWriterFactory jsonWriterFactory;

    public JsonPHTTPMessageCodec() {
        this(emptyMap());
    }

    public JsonPHTTPMessageCodec(Map<String, ?> jsonConfiguration) {
        this(Json.createReaderFactory(jsonConfiguration), Json.createWriterFactory(jsonConfiguration));
    }

    public JsonPHTTPMessageCodec(JsonReaderFactory jsonReaderFactory) {
        this(jsonReaderFactory, Json.createWriterFactory(emptyMap()));
    }

    public JsonPHTTPMessageCodec(JsonWriterFactory jsonWriterFactory) {
        this(Json.createReaderFactory(emptyMap()), jsonWriterFactory);
    }

    public JsonPHTTPMessageCodec(JsonReaderFactory jsonReaderFactory, JsonWriterFactory jsonWriterFactory) {
        this.jsonReaderFactory = jsonReaderFactory;
        this.jsonWriterFactory = jsonWriterFactory;
    }


    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.compatible(JsonStructure.class);
    }

    @Override
    public HTTPRequestBody write(JsonStructure body, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_JSON, () -> serialize(body, encoding));
    }

    private BodyPublisher serialize(JsonStructure body, Charset encoding) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(output, encoding);
             JsonWriter jsonWriter = jsonWriterFactory.createWriter(writer)) {

            jsonWriter.write(body);

            writer.flush();

            return BodyPublishers.ofByteArray(output.toByteArray());

        } catch (JsonException | IOException e) {
            throw new HTTPRequestWriterException("JSON serialization failed. Source: " + body, e);
        }
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return javaType.classType().map(JsonStructure.class::isAssignableFrom).orElse(false);
    }

    @Override
    public Optional<CompletableFuture<JsonStructure>> read(HTTPResponseBody body, JavaType javaType) {
        return body.readAsInputStream(this::deserialize);
    }

    private JsonStructure deserialize(InputStream bodyAsStream) {
        try (InputStreamReader reader = new InputStreamReader(bodyAsStream);
             BufferedReader buffered = new BufferedReader(reader);
             JsonReader jsonReader = jsonReaderFactory.createReader(buffered)) {

            return jsonReader.read();
        } catch (JsonException | IOException e) {
            throw new HTTPResponseReaderException("JSON deserialization failed.", e);
        }
    }

    public static JsonPHTTPMessageCodec provider() {
        return SINGLE_INSTANCE;
    }
}
