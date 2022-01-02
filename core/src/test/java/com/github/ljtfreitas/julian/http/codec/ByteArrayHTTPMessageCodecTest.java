package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.JavaType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ByteArrayHTTPMessageCodecTest {

	private final ByteArrayHTTPMessageCodec codec = new ByteArrayHTTPMessageCodec();

	@Nested
	class Readable {

		@Test
		void supported() {
			assertTrue(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(byte[].class)));
		}

		@Test
		void unsupported() {
			assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}

	@Test
	void read() {
		byte[] expected = "response body".getBytes();
		assertArrayEquals(expected, codec.read(expected, JavaType.valueOf(byte[].class)));
	}

	@Nested
	class Writable {

		@Test
		void supported() {
			assertTrue(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(byte[].class)));
		}

		@Test
		void unsupported() {
			assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}

	@Test
	void write() {
		byte[] expected = "response body".getBytes();

		HTTPRequestBody httpRequestBody = codec.write(expected, UTF_8);

		assertTrue(httpRequestBody.contentType().isEmpty());

		httpRequestBody.serialize().subscribe(new Subscriber<>() {

			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(1);
			}

			@Override
			public void onNext(ByteBuffer item) {
				assertArrayEquals(expected, item.array());
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
