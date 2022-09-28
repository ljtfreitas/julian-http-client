package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Subscriber;
import com.github.ljtfreitas.julian.http.Download;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

class DownloadHTTPResponseReaderTest {

    private final DownloadHTTPResponseReader reader = new DownloadHTTPResponseReader();

    private final String expected = "i am a response body...";
    private final HTTPResponseBody body = HTTPResponseBody.some(expected.getBytes());

    @Test
    void downloadAsBytes() {
        Optional<CompletableFuture<Download>> future = reader.read(body, JavaType.valueOf(Download.class));

        Download download = future.map(CompletableFuture::join).orElseThrow();

        String content = download.readAsBytes().then(String::new).join().unsafe();

        assertThat(content, equalTo(expected));
    }

    @Test
    void downloadAsInputStream() {
        Optional<CompletableFuture<Download>> future = reader.read(body, JavaType.valueOf(Download.class));

        Download download = future.map(CompletableFuture::join).orElseThrow();

        String content = download.readAsInputStream()
                .then(i -> Bracket.acquire(() -> i)
                        .map(InputStream::readAllBytes)
                        .map(String::new)
                        .unsafe())
                .join()
                .unsafe();

        assertThat(content, equalTo(expected));
    }

    @Test
    void downloadAsBuffer() {
        Optional<CompletableFuture<Download>> future = reader.read(body, JavaType.valueOf(Download.class));

        Download download = future.map(CompletableFuture::join).orElseThrow();

        String content = download.readAsBuffer()
                .then(ByteBuffer::array)
                .then(String::new)
                .join()
                .unsafe();

        assertThat(content, equalTo(expected));
    }

    @Test
    void subscribeToListOfBuffers() {
        Optional<CompletableFuture<Download>> future = reader.read(body, JavaType.valueOf(Download.class));

        Download download = future.map(CompletableFuture::join).orElseThrow();

        download.subscribe(new Subscriber<>() {

            private final List<ByteBuffer> items = new ArrayList<>();

            @Override
            public void success(List<ByteBuffer> value) {
                items.addAll(value);
            }

            @Override
            public void failure(Throwable failure) {
                fail(failure);
            }

            @Override
            public void done() {
                ByteArrayOutputStream output = items.stream().reduce(new ByteArrayOutputStream(),
                        (o, b) -> {
                            o.write(b.array(), 0, b.array().length);
                            return o;
                        },
                        (a, b) -> b);

                Attempt.just(output::flush)
                        .map(none -> output.toByteArray())
                        .map(String::new)
                        .onSuccess(content -> assertThat(content, equalTo(expected)));
            }
        }).dispose();
    }

    @Test
    void writeToFile() throws IOException {
        Optional<CompletableFuture<Download>> future = reader.read(body, JavaType.valueOf(Download.class));

        Download download = future.map(CompletableFuture::join).orElseThrow();

        Path file = Files.createTempFile("sample", ".txt").toAbsolutePath();

        Path downloaded = download.writeTo(file, StandardOpenOption.WRITE).join().unsafe();

        String content = Files.readString(downloaded);

        assertThat(content, equalTo(expected));
    }

}