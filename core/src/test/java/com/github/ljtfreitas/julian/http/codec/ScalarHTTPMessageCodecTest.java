package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ScalarHTTPMessageCodecTest {

	private final ScalarHTTPMessageCodec codec = new ScalarHTTPMessageCodec();

	@Nested
	class Readable {

		@Test
		void unsupported() {
			assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
		
		@ParameterizedTest
		@ArgumentsSource(ScalarTypesProvider.class)
		void supported(Class<?> scalarClassType) {
			assertTrue(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(scalarClassType)));
		}
		
		@Nested
		class Read {
			
			@ParameterizedTest
			@ArgumentsSource(ScalarTypesProvider.class)
			void read(Class<?> scalarClassType, Object value) {
				
				Object output = codec.read(HTTPResponseBody.some(value.toString().getBytes()), JavaType.valueOf(scalarClassType))
						.map(CompletableFuture::join)
						.orElse(null);
				
				assertAll(() -> assertEquals(value, output),
						  () -> assertThat(output, instanceOf(scalarClassType)));
			}
		}
	}
	
	@Nested
	class Writable {

		@Test
		void unsupported() {
			assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
		
		@ParameterizedTest
		@ArgumentsSource(ScalarTypesProvider.class)
		void supported(Class<?> scalarClassType) {
			assertTrue(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(scalarClassType)));
		}

		@Nested
		class Write {
			
			@ParameterizedTest
			@ArgumentsSource(ScalarTypesProvider.class)
			void write(Class<?> scalarClassType, Object value) {
				HTTPRequestBody output = codec.write(value, StandardCharsets.UTF_8);

				output.serialize().subscribe(new Flow.Subscriber<>() {

					@Override
					public void onSubscribe(Flow.Subscription subscription) {
						subscription.request(1);
					}

					@Override
					public void onNext(ByteBuffer item) {
						assertArrayEquals(value.toString().getBytes(), item.array());
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

	static class ScalarTypesProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return Stream.of(Arguments.of(byte.class, (byte) 1),
							 Arguments.of(short.class, (short) 1),
							 Arguments.of(int.class, 1),
							 Arguments.of(long.class, 1l),
							 Arguments.of(float.class, 1.1f),
							 Arguments.of(double.class, 1.1d),
							 Arguments.of(boolean.class, true),
							 Arguments.of(char.class, 'a'),
							 Arguments.of(Byte.class, Byte.valueOf((byte) 1)),
							 Arguments.of(Short.class, Short.valueOf((short) 1)),
							 Arguments.of(Integer.class, Integer.valueOf(1)),
							 Arguments.of(Long.class, Long.valueOf(1l)),
							 Arguments.of(Float.class, Float.valueOf(1.1f)),
							 Arguments.of(Double.class, Double.valueOf(1.1d)),
							 Arguments.of(Boolean.class, Boolean.TRUE),
							 Arguments.of(Character.class, Character.valueOf('a')));
		}
	}
}
