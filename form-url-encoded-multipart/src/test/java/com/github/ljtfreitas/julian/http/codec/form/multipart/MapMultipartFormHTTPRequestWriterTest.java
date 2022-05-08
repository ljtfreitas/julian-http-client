package com.github.ljtfreitas.julian.http.codec.form.multipart;

import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MapMultipartFormHTTPRequestWriterTest {

    private final MapMultipartFormHTTPRequestWriter writer = new MapMultipartFormHTTPRequestWriter(() -> "abc1234");

    @Test
    void serialize() {
        String expected = "--abc1234"
                + "\r\n"
                + "Content-Disposition: form-data; name=\"name\""
                + "\r\n"
                + "Content-Type: text/plain"
                + "\r\n"
                + "\r\n"
                + "Tiago de Freitas Lima"
                + "\r\n"
                + "--abc1234"
                + "\r\n"
                + "Content-Disposition: form-data; name=\"age\""
                + "\r\n"
                + "Content-Type: text/plain"
                + "\r\n"
                + "\r\n"
                + "35"
                + "\r\n"
                + "\r\n"
                + "--abc1234--";

        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "Tiago de Freitas Lima");
        form.put("age", "35");

        HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

        assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

        httpRequestBody.serialize().subscribe(new Subscriber<>() {

            private ByteBuffer item = ByteBuffer.allocate(0);

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(ByteBuffer item) {
                this.item = item;
            }

            @Override
            public void onError(Throwable throwable) {
                fail(throwable);
            }

            @Override
            public void onComplete() {
                assertEquals(expected, new String(item.array()));
            }
        });
    }
}