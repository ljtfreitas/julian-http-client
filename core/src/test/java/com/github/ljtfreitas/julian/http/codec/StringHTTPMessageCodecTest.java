package com.github.ljtfreitas.julian.http.codec;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.JavaType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringHTTPMessageCodecTest {

    StringHTTPMessageCodec codec = new StringHTTPMessageCodec();

    @Nested
    class Readable {

        @Test
        void unsupported() {
            assertFalse(codec.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void supported() {
            assertTrue(codec.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(String.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "response body";

                Object output = codec.read(new ByteArrayInputStream(value.getBytes()), JavaType.valueOf(String.class));

                assertEquals(value, output);
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
            assertTrue(codec.writable(ContentType.valueOf("text/plain"), String.class));
        }

        @Nested
        class Write {

            @Test
            void write() {
                String value = "request body";

                byte[] output = codec.write(value, StandardCharsets.UTF_8);

                assertArrayEquals(value.getBytes(), output);
            }
        }
    }
}