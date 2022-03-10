package com.github.ljtfreitas.julian.http.codec.form;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.Form;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
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

                Map<String, ? extends Iterable<?>> form = codec.read(HTTPResponseBody.some(value.getBytes()), JavaType.valueOf(Form.class))
                        .map(CompletableFuture::join)
                        .orElse(Collections.emptyMap());

                assertThat(form, allOf(hasKey("name"), hasKey("name")));
                assertThat(form.get("name"), contains("Tiago"));
                assertThat(form.get("age"), contains("35"));
            }
        }
    }
}