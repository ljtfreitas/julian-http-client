package com.github.ljtfreitas.julian.http.codec.form.multipart;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class FileUploadHTTPRequestWriterTest {

    private final FileUploadHTTPRequestWriter writer = new FileUploadHTTPRequestWriter(() -> "abc1234");

    private final File file = Attempt.run(() -> new File(getClass().getResource("/sample-file.txt").toURI())).prop();

    @Test
    void serialize() throws IOException {
        String fileContent = Files.readString(file.toPath());

        String expected = "--abc1234"
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"sample-file.txt\""
                + "\r\n"
                + "Content-Type: text/plain"
                + "\r\n"
                + "\r\n"
                + fileContent
                + "\r\n"
                + "--abc1234--";

        HTTPRequestBody httpRequestBody = writer.write(file, UTF_8);

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