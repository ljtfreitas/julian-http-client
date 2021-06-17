package com.github.ljtfreitas.julian.http.codec.json.gson;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.codec.ContentType;
import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GsonJsonHTTPMessageCodecTest {

    private GsonJsonHTTPMessageCodec<Person> codec = new GsonJsonHTTPMessageCodec<>(new Gson());

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

        final String name;
        final int age;

        private Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}