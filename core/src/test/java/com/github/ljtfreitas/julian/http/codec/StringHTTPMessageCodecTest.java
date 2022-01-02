package com.github.ljtfreitas.julian.http.codec;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.JavaType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class StringHTTPMessageCodecTest {

    private final StringHTTPMessageCodec codec = new StringHTTPMessageCodec();

    @Nested
    class Readable {

        @Test
        void unsupported() {
            assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void textPlain() {
            assertTrue(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
        }

        @Test
        void any() {
            assertTrue(codec.readable(MediaType.valueOf("application/xml"), JavaType.valueOf(String.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "response body";

                Object output = codec.read(value.getBytes(), JavaType.valueOf(String.class));

                assertEquals(value, output);
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
            assertTrue(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
        }

        @Nested
        class Write {

            @Test
            void write() {
                String value = "request body";

                HTTPRequestBody output = codec.write(value, StandardCharsets.UTF_8);

                output.serialize().subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(ByteBuffer item) {
                        assertArrayEquals(value.getBytes(), item.array());
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
}