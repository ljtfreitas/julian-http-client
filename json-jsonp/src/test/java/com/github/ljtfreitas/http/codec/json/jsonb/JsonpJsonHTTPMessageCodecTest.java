package com.github.ljtfreitas.http.codec.json.jsonb;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.codec.ContentType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonpJsonHTTPMessageCodecTest {

    private JsonpJsonHTTPMessageCodec codec = new JsonpJsonHTTPMessageCodec();

    @Nested
    class AsReader {

        @Test
        @DisplayName("It should not support content types that are not application/json")
        void shouldNotSupportContentTypesThatAreNotApplicationJson() {
            assertFalse(codec.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @ParameterizedTest
        @ArgumentsSource(SupportedJsonStructureTypesProvider.class)
        @DisplayName("It should support application/json as content type and classes compatible with JsonStructure")
        void shouldSupportApplicationJsonAsContentTypeWithJsonStructureTypes(Class<?> supportedType) {
            assertTrue(codec.readable(ContentType.valueOf("application/json"), JavaType.valueOf(supportedType)));
        }

        @Test
        @DisplayName("It should be able to read a JsonObject value")
        void shouldBeAbleToReadJsonObjectValue() {
            String value = "{\"name\":\"Tiago\",\"age\":35}";

            JsonStructure jsonStructure = codec.read(new ByteArrayInputStream(value.getBytes()), JavaType.valueOf(JsonObject.class));

            assertTrue(jsonStructure instanceof JsonObject);

            JsonObject jsonObject = (JsonObject) jsonStructure;

            assertAll(() -> assertEquals(35, jsonObject.getInt("age")),
                      () -> assertEquals("Tiago", jsonObject.getString("name")));
        }

        @Test
        @DisplayName("It should be able to read a JsonArray value")
        void shouldBeAbleToReadJsonArrayValue() {
            String value = "[{\"name\":\"Tiago\",\"age\":35}]";

            JsonStructure jsonStructure = codec.read(new ByteArrayInputStream(value.getBytes()), JavaType.valueOf(JsonArray.class));

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
        void shouldNotSupportContentTypesThatAreNotApplicationJson() {
            assertFalse(codec.writable(ContentType.valueOf("text/plain"), Object.class));
        }

        @ParameterizedTest
        @ArgumentsSource(SupportedJsonStructureTypesProvider.class)
        @DisplayName("It should support application/json as content type and classes compatible with JsonStructure")
        void shouldSupportApplicationJsonAsContentTypeWithJsonStructureTypes(Class<?> supportedType) {
            assertTrue(codec.writable(ContentType.valueOf("application/json"), supportedType));
        }

        @Test
        @DisplayName("It should be able to write a JsonObject value")
        void shouldBeAbleToWriteJsonObjectValue() {
            JsonObject jsonObject = Json.createObjectBuilder().add("name", "Tiago").add("age", 35).build();

            byte[] output = codec.write(jsonObject, StandardCharsets.UTF_8);

            assertEquals(jsonObject.toString(), new String(output));
        }

        @Test
        @DisplayName("It should be able to write a JsonArray value")
        void shouldBeAbleToWriteJsonArrayValue() {
            JsonArray jsonArray = Json.createArrayBuilder()
                    .add(0, Json.createObjectBuilder().add("name", "Tiago").add("age", 35).build())
                    .build();

            byte[] output = codec.write(jsonArray, StandardCharsets.UTF_8);

            assertEquals(jsonArray.toString(), new String(output));
        }
    }

    static class SupportedJsonStructureTypesProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of(JsonStructure.class), Arguments.of(JsonArray.class), Arguments.of(JsonObject.class));
        }
    }
}