package com.github.ljtfreitas.julian.http.codec.form.multipart;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.multipart.MultipartForm;
import com.github.ljtfreitas.julian.multipart.MultipartForm.Part;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import static com.github.ljtfreitas.julian.http.MediaType.MULTIPART_FORM_DATA;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MultipartFormObjectHTTPRequestWriterTest {

    private final MultipartFormObjectHTTPRequestWriter writer = new MultipartFormObjectHTTPRequestWriter(() -> "abc1234");

    @Test
    void shouldSupports() {
        assertTrue(writer.writable(MULTIPART_FORM_DATA, JavaType.valueOf(MultipartForm.class)));
    }

    @Test
    void shouldNotSupports() {
        assertFalse(writer.writable(MULTIPART_FORM_DATA, JavaType.parameterized(Map.class, String.class, String.class)));
    }

    @Nested
    class Serialization {

        @Test
        void simple() {
            String expected = "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "Tiago de Freitas Lima"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"age\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "35"
                    + "\r\n"
                    + "\r\n"
                    + "----abc1234--";

            MultipartForm form = new MultipartForm()
                    .join(Part.create("name", "Tiago de Freitas Lima"))
                    .join(Part.create("age", "35"));

            HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

            assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

            httpRequestBody.serialize().subscribe(new MultipartFormBodySubscriber(expected));
        }

        @Test
        void file() throws URISyntaxException {
            String expected = "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "Tiago de Freitas Lima"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"age\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "35"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"my-file\"; filename=\"sample-file.txt\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "this is a simple text file."
                    + "\n"
                    + "if you can send this content in a multipart request, congrats. the code is working."
                    + "\r\n"
                    + "\r\n"
                    + "----abc1234--";

            MultipartForm form = new MultipartForm()
                    .join(Part.create("name", "Tiago de Freitas Lima"))
                    .join(Part.create("age", "35"))
                    .join(Part.create("my-file", new File(getClass().getResource("/sample-file.txt").toURI())));

            HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

            assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

            httpRequestBody.serialize().subscribe(new MultipartFormBodySubscriber(expected));
        }

        @Test
        void path() throws URISyntaxException {
            String expected = "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "Tiago de Freitas Lima"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"age\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "35"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"my-file\"; filename=\"sample-file.txt\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "this is a simple text file."
                    + "\n"
                    + "if you can send this content in a multipart request, congrats. the code is working."
                    + "\r\n"
                    + "\r\n"
                    + "----abc1234--";

            MultipartForm form = new MultipartForm()
                    .join(Part.create("name", "Tiago de Freitas Lima"))
                    .join(Part.create("age", "35"))
                    .join(Part.create("my-file", Path.of(getClass().getResource("/sample-file.txt").toURI())));

            HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

            assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

            httpRequestBody.serialize().subscribe(new MultipartFormBodySubscriber(expected));
        }

        @Test
        void fileAsByteArray() throws URISyntaxException, IOException {
            byte[] fileAsBytes = Files.readAllBytes(Path.of(getClass().getResource("/image.jpeg").toURI()));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(("----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "Tiago de Freitas Lima"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"age\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "35"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"my-file\""
                    + "\r\n"
                    + "Content-Type: image/jpeg"
                    + "\r\n"
                    + "\r\n").getBytes());
            output.write(fileAsBytes);
            output.write(("\r\n"
                    + "\r\n"
                    + "----abc1234--").getBytes());
            output.flush();
            output.close();

            MultipartForm form = new MultipartForm()
                    .join(Part.create("name", "Tiago de Freitas Lima"))
                    .join(Part.create("age", "35"))
                    .join(Part.create("my-file", fileAsBytes, MediaType.valueOf("image/jpeg")));

            HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

            assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

            MultipartFormBodyCollector collector = new MultipartFormBodyCollector();
            httpRequestBody.serialize().subscribe(collector);

            assertArrayEquals(output.toByteArray(), collector.all);
        }

        @Test
        void fileAsInputStream() throws URISyntaxException, IOException {
            InputStream source = new BufferedInputStream(Files.newInputStream(Path.of(getClass().getResource("/image.jpeg").toURI())));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(("----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "Tiago de Freitas Lima"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"age\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "35"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"my-file\""
                    + "\r\n"
                    + "Content-Type: image/jpeg"
                    + "\r\n"
                    + "\r\n").getBytes());
            source.transferTo(output);
            output.write(("\r\n"
                    + "\r\n"
                    + "----abc1234--").getBytes());
            output.flush();
            output.close();

            InputStream fileAsInputStream = Files.newInputStream(Path.of(getClass().getResource("/image.jpeg").toURI()));

            MultipartForm form = new MultipartForm()
                    .join(Part.create("name", "Tiago de Freitas Lima"))
                    .join(Part.create("age", "35"))
                    .join(Part.create("my-file", fileAsInputStream, MediaType.valueOf("image/jpeg")));

            HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

            assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

            MultipartFormBodyCollector collector = new MultipartFormBodyCollector();

            httpRequestBody.serialize().subscribe(collector);

            assertArrayEquals(output.toByteArray(), collector.all);
        }

        @Test
        void fileAsByteBuffer() throws URISyntaxException, IOException {
            ByteBuffer fileAsByteBuffer = ByteBuffer.wrap(Files.readAllBytes(Path.of(getClass().getResource("/image.jpeg").toURI())));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(("----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "Tiago de Freitas Lima"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"age\""
                    + "\r\n"
                    + "Content-Type: text/plain"
                    + "\r\n"
                    + "\r\n"
                    + "35"
                    + "\r\n"
                    + "----abc1234"
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"my-file\""
                    + "\r\n"
                    + "Content-Type: image/jpeg"
                    + "\r\n"
                    + "\r\n").getBytes());
            output.write(fileAsByteBuffer.array());
            output.write(("\r\n"
                    + "\r\n"
                    + "----abc1234--").getBytes());
            output.flush();
            output.close();

            fileAsByteBuffer.rewind();

            MultipartForm form = new MultipartForm()
                    .join(Part.create("name", "Tiago de Freitas Lima"))
                    .join(Part.create("age", "35"))
                    .join(Part.create("my-file", fileAsByteBuffer, MediaType.valueOf("image/jpeg")));

            HTTPRequestBody httpRequestBody = writer.write(form, UTF_8);

            assertEquals("multipart/form-data; boundary=abc1234", httpRequestBody.contentType().map(Object::toString).orElse(""));

            MultipartFormBodyCollector collector = new MultipartFormBodyCollector();

            httpRequestBody.serialize().subscribe(collector);

            assertArrayEquals(output.toByteArray(), collector.all);
        }
    }

    private class MultipartFormBodySubscriber implements Subscriber<ByteBuffer> {

        private final Collection<ByteBuffer> received = new ArrayList<>();
        private final String expected;

        private Subscription subscription;

        private MultipartFormBodySubscriber(String expected) {
            this.expected = expected;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer item) {
            received.add(item);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            fail(throwable);
        }

        @Override
        public void onComplete() {
            int size = received.stream().mapToInt(Buffer::remaining).sum();

            ByteBuffer bytes = ByteBuffer.allocate(size);
            received.forEach(bytes::put);

            String bodyAsString = new String(bytes.array());

            assertEquals(expected, bodyAsString);
        }
    }

    private class MultipartFormBodyCollector implements Subscriber<ByteBuffer> {

        private final Collection<ByteBuffer> received = new ArrayList<>();

        private Subscription subscription;
        private byte[] all = null;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer item) {
            received.add(item);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            fail(throwable);
        }

        @Override
        public void onComplete() {
            int size = received.stream().mapToInt(Buffer::remaining).sum();

            ByteBuffer bytes = ByteBuffer.allocate(size);
            received.forEach(bytes::put);

            this.all = bytes.array();
        }
    }
}