package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class InputStreamHTTPMessageCodecTest {

	private final InputStreamHTTPMessageCodec codec = new InputStreamHTTPMessageCodec();

	@Nested
	class Readable {
		
		@Test
		void supported() {
			assertTrue(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(InputStream.class)));
		}
	
		@Test
		void unsupported() {
			assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}
	
	@Test
	void read() {
		String expected = "response body";

		InputStream in = codec.read(expected.getBytes(), JavaType.valueOf(InputStream.class));

		String actual = Bracket.acquire(( ) -> new BufferedReader(new InputStreamReader(in)))
			.map(r -> r.lines().collect(joining()))
			.unsafe();
		
		assertEquals(expected, actual);
	}

	@Nested
	class Writable {

		@Test
		void supported() {
			assertTrue(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(InputStream.class)));
		}

		@Test
		void unsupported() {
			assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}

	@Test
	void write() {
		byte[] source = "response body".getBytes();
		InputStream expected = new ByteArrayInputStream(source);

		HTTPRequestBody httpRequestBody = codec.write(expected, UTF_8);

		assertTrue(httpRequestBody.contentType().isEmpty());

		httpRequestBody.serialize().subscribe(new Subscriber<>() {

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(1);
			}

			@Override
			public void onNext(ByteBuffer item) {
				assertEquals(new String(source), new String(item.array()).trim());
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
