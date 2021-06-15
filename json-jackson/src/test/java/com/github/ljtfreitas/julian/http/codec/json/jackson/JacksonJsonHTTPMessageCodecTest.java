package com.github.ljtfreitas.julian.http.codec.json.jackson;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.codec.ContentType;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonJsonHTTPMessageCodecTest {

    JacksonJsonHTTPMessageCodec<Person> codec = new JacksonJsonHTTPMessageCodec<>(new ObjectMapper());

    @Nested
    class Readable {

        @Test
        void unsupported() {
            assertFalse(codec.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void supported() {
            assertTrue(codec.readable(ContentType.valueOf("application/json"), JavaType.valueOf(Person.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "{\"name\":\"Tiago\",\"age\":35}";

                Person person = codec.read(new ByteArrayInputStream(value.getBytes()), JavaType.valueOf(Person.class));

                assertAll(() -> assertEquals("Tiago", person.name),
                          () -> assertEquals(35, person.age));
            }
        }
    }

    @Nested
    class Writable {

        @Test
        void unsupported() {
            assertFalse(codec.writable(ContentType.valueOf("text/plain"), Object.class));
        }

        @Test
        void supported() {
            assertTrue(codec.writable(ContentType.valueOf("application/json"), Person.class));
        }

        @Nested
        class Write {

            @Test
            void write() {
                byte[] output = codec.write(new Person("Tiago", 35), StandardCharsets.UTF_8);

                assertEquals("{\"name\":\"Tiago\",\"age\":35}", new String(output));
            }
        }
    }

    private static class Person {

        @JsonProperty
        final String name;

        @JsonProperty
        final int age;

        @JsonCreator
        private Person(@JsonProperty("name") String name, @JsonProperty("age") int age) {
            this.name = name;
            this.age = age;
        }
    }
}