package com.github.ljtfreitas.julian.http.codec.json.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JacksonJsonHTTPMessageCodecTest {

    private final JacksonJsonHTTPMessageCodec codec = new JacksonJsonHTTPMessageCodec();

    @Nested
    class Readable {

        @Test
        void unsupported() {
            assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void supported() {
            assertTrue(codec.readable(MediaType.valueOf("application/json"), JavaType.valueOf(Person.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "{\"name\":\"Tiago\",\"age\":35}";

                Person person = (Person) codec.read(HTTPResponseBody.some(value.getBytes()), JavaType.valueOf(Person.class))
                        .map(CompletableFuture::join)
                        .orElse(null);

                assertAll(() -> assertEquals("Tiago", person.name),
                          () -> assertEquals(35, person.age));
            }
        }
    }

    @Nested
    class Writable {

        @Test
        void unsupported() {
            assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void supported() {
            assertTrue(codec.writable(MediaType.valueOf("application/json"), JavaType.valueOf(Person.class)));
        }

        @Nested
        class Write {

            @Test
            void write() {
                HTTPRequestBody output = codec.write(new Person("Tiago", 35), StandardCharsets.UTF_8);

                assertEquals(APPLICATION_JSON, output.contentType().orElseThrow());

                output.serialize().subscribe(new Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(ByteBuffer item) {
                        assertEquals("{\"name\":\"Tiago\",\"age\":35}", new String(item.array()));
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