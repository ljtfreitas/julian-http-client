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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_FORM_URLENCODED;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MultiMapFormURLEncodedHTTPMessageCodecTest {

    private final MultiMapFormURLEncodedHTTPMessageCodec codec = new MultiMapFormURLEncodedHTTPMessageCodec();

    @Nested
    class Writable {

        @Test
        void unsupported() {
            assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void mapOfStringIsNotSupported() {
            assertFalse(codec.writable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, String.class)));
        }

        @Test
        void mapOfCollectionIsSupported() {
            assertTrue(codec.writable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, Collection.class)));
        }

        @Nested
        class Write {

            @Test
            void write() {
                HTTPRequestBody output = codec.write(Map.of("name", List.of("Tiago de Freitas Lima"), "age", List.of(35)), StandardCharsets.UTF_8);

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
        void mapOfStringIsNotSupported() {
            assertFalse(codec.readable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, String.class)));
        }

        @Test
        void mapOfCollectionIsSupported() {
            assertTrue(codec.readable(MediaType.valueOf("application/x-www-form-urlencoded"), JavaType.parameterized(Map.class, String.class, Collection.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "name=Tiago&age=35";

                Map<String, Collection<String>> form = codec.read(value.getBytes(), JavaType.valueOf(Form.class));

                assertAll(() -> assertEquals("Tiago", form.getOrDefault("name", emptyList()).stream().findFirst().orElse("")),
                          () -> assertEquals("35", form.getOrDefault("age", emptyList()).stream().findFirst().orElse("")));
            }
        }
    }
}