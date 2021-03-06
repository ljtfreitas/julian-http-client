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
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JsonPHTTPMessageCodecTest {

    private final JsonPHTTPMessageCodec codec = new JsonPHTTPMessageCodec();

    @Nested
    class AsReader {

        @Test
        @DisplayName("It should not support content types that are not application/json")
        void shouldNotSupportMediaTypesThatAreNotApplicationJson() {
            assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @ParameterizedTest
        @ArgumentsSource(SupportedJsonStructureTypesProvider.class)
        @DisplayName("It should support application/json as content type and classes compatible with JsonStructure")
        void shouldSupportApplicationJsonAsMediaTypeWithJsonStructureTypes(Class<?> supportedType) {
            assertTrue(codec.readable(MediaType.valueOf("application/json"), JavaType.valueOf(supportedType)));
        }

        @Test
        @DisplayName("It should be able to read a JsonObject value")
        void shouldBeAbleToReadJsonObjectValue() {
            String value = "{\"name\":\"Tiago\",\"age\":35}";

            JsonStructure jsonStructure = codec.read(HTTPResponseBody.some(value.getBytes()), JavaType.valueOf(JsonObject.class))
                    .map(CompletableFuture::join)
                    .orElse(null);

            assertTrue(jsonStructure instanceof JsonObject);

            JsonObject jsonObject = (JsonObject) jsonStructure;

            assertAll(() -> assertEquals(35, jsonObject.getInt("age")),
                      () -> assertEquals("Tiago", jsonObject.getString("name")));
        }

        @Test
        @DisplayName("It should be able to read a JsonArray value")
        void shouldBeAbleToReadJsonArrayValue() {
            String value = "[{\"name\":\"Tiago\",\"age\":35}]";

            JsonStructure jsonStructure = codec.read(HTTPResponseBody.some(value.getBytes()), JavaType.valueOf(JsonArray.class))
                    .map(CompletableFuture::join)
                    .orElse(null);

            assertTrue(jsonStructure instanceof JsonArray);

            JsonArray jsonArray = (JsonArray) jsonStructure;

            assertThat(jsonArray, hasSize(1));

            JsonObject jsonObject = jsonArray.get(0).asJsonObject();

            assertAll(() -> assertEquals(35, jsonObject.getInt("age")),
                      () -> assertEquals("Tiago", jsonObject.getString("name")));
        }
    }

    @Nested
    class AsWriter {

        @Test
        @DisplayName("It should not support content types that are not application/json")
        void shouldNotSupportMediaTypesThatAreNotApplicationJson() {
            assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @ParameterizedTest
        @ArgumentsSource(SupportedJsonStructureTypesProvider.class)
        @DisplayName("It should support application/json as content type and classes compatible with JsonStructure")
        void shouldSupportApplicationJsonAsMediaTypeWithJsonStructureTypes(Class<?> supportedType) {
            assertTrue(codec.writable(MediaType.valueOf("application/json"), JavaType.valueOf(supportedType)));
        }

        @Test
        @DisplayName("It should be able to write a JsonObject value")
        void shouldBeAbleToWriteJsonObjectValue() {
            JsonObject jsonObject = Json.createObjectBuilder().add("name", "Tiago").add("age", 35).build();

            HTTPRequestBody output = codec.write(jsonObject, StandardCharsets.UTF_8);

            assertEquals(APPLICATION_JSON, output.contentType().orElseThrow());

            output.serialize().subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(1);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    assertEquals(jsonObject.toString(), new String(item.array()));
                }

                @Override
                public void onError(Throwable throwable) {
                    fail(throwable);
                }

                @Override
                public void onComplete() {
                }
            });
        }

        @Test
        @DisplayName("It should be able to write a JsonArray value")
        void shouldBeAbleToWriteJsonArrayValue() {
            JsonArray jsonArray = Json.createArrayBuilder()
                    .add(0, Json.createObjectBuilder().add("name", "Tiago").add("age", 35).build())
                    .build();

            HTTPRequestBody output = codec.write(jsonArray, StandardCharsets.UTF_8);

            assertEquals(APPLICATION_JSON, output.contentType().orElseThrow());

            output.serialize().subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(1);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    assertEquals(jsonArray.toString(), new String(item.array()));
                }

                @Override
                public void onError(Throwable throwable) {
                    fail(throwable);
                }

                @Override
                public void onComplete() {
                }
            });

        }
    }

    static class SupportedJsonStructureTypesProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(Arguments.of(JsonStructure.class), Arguments.of(JsonArray.class), Arguments.of(JsonObject.class));
        }
    }
}