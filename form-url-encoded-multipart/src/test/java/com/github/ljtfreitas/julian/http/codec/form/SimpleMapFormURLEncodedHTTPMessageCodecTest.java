package com.github.ljtfreitas.julian.http.codec.form;

import com.github.ljtfreitas.julian.Form;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SimpleMapFormURLEncodedHTTPMessageCodecTest {

    private final SimpleMapFormURLEncodedHTTPMessageCodec codec = new SimpleMapFormURLEncodedHTTPMessageCodec();

    @Nested
    class Writable {

        @Test
        void unsupported() {
            assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void mapOfCollectionIsNotSupported() {
            assertFalse(codec.writable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, Collection.class)));
        }

        @Test
        void mapOfStringIsSupported() {
            assertTrue(codec.writable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, String.class)));
        }

        @Nested
        class Write {

            @Test
            void write() {
                HTTPRequestBody output = codec.write(Map.of("name", "Tiago de Freitas Lima", "age", "35"), StandardCharsets.UTF_8);

                assertEquals(APPLICATION_FORM_URLENCODED, output.contentType().orElseThrow());

                output.serialize().subscribe(new Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(ByteBuffer item) {
                        assertEquals("name=" + URLEncoder.encode("Tiago de Freitas Lima", StandardCharsets.UTF_8) + "&age=35",
                                new String(item.array()));
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

    @Nested
    class Readable {

        @Test
        void unsupported() {
            assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void mapOfCollectionIsNotSupported() {
            assertFalse(codec.readable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, Collection.class)));
        }

        @Test
        void mapOfStringIsSupported() {
            assertTrue(codec.readable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, String.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "name=Tiago&age=35";

                Map<String, String> form = codec.read(value.getBytes(), JavaType.valueOf(Form.class));

                assertAll(() -> assertEquals("Tiago", form.get("name")),
                          () -> assertEquals("35", form.get("age")));
            }
        }
    }
}