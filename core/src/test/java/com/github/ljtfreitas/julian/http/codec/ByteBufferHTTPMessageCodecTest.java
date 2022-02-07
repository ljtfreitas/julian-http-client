package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ByteBufferHTTPMessageCodecTest {

	private final ByteBufferHTTPMessageCodec codec = new ByteBufferHTTPMessageCodec();

	@Nested
	class Readable {

		@Test
		void supported() {
			assertTrue(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(ByteBuffer.class)));
		}

		@Test
		void unsupported() {
			assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}

	@Test
	void read() {
		byte[] source = "response body".getBytes();
		assertArrayEquals(ByteBuffer.wrap(source).array(), codec.read(HTTPResponseBody.some(source), JavaType.valueOf(ByteBuffer.class))
				.map(CompletableFuture::join)
				.map(ByteBuffer::array)
				.orElse(new byte[0]));
	}

	@Nested
	class Writable {

		@Test
		void supported() {
			assertTrue(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(ByteBuffer.class)));
		}

		@Test
		void unsupported() {
			assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}

	@Test
	void write() {
		ByteBuffer expected = ByteBuffer.wrap("response body".getBytes());

		HTTPRequestBody httpRequestBody = codec.write(expected, UTF_8);

		assertTrue(httpRequestBody.contentType().isEmpty());

		httpRequestBody.serialize().subscribe(new Subscriber<>() {

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(1);
			}

			@Override
			public void onNext(ByteBuffer item) {
				assertArrayEquals(expected.array(), item.array());
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
