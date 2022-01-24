package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LazyResponseTTest {

	@Mock
	private Endpoint endpoint;

	private final LazyResponseT<String> responseT = new LazyResponseT<>();

	@Nested
	class Predicates {

		@Test
		void supported() {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(Lazy.class));

			assertTrue(responseT.test(endpoint));
		}

		@Test
		void unsupported() {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

			assertFalse(responseT.test(endpoint));
		}
	}

	@Nested
	class Adapted {

		@Test
		void parameterized() {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Lazy.class, String.class));

			assertEquals(JavaType.valueOf(String.class), responseT.adapted(endpoint));
		}

		@Test
		void whenLazyIsNotParameterizedMustUseObject() {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(Lazy.class));

			assertEquals(JavaType.valueOf(Object.class), responseT.adapted(endpoint));
		}
	}

	@Test
	void compose(@Mock ResponseFn<String, String> fn, @Mock Promise<Response<String, Exception>, Exception> response) {
		Arguments arguments = Arguments.empty();

		when(fn.run(response, arguments)).thenReturn(Promise.done("expected"));

		Lazy<String> lazy = responseT.bind(endpoint, fn).join(response, arguments);

		assertEquals("expected", lazy.run());
	}
}
