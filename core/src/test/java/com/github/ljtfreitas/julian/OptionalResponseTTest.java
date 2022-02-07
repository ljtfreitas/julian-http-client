package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionalResponseTTest {

	@Mock
	private Endpoint endpoint;

	private final OptionalResponseT<String> responseT = new OptionalResponseT<>();

	@Nested
	class Predicates {

		@Test
		void supported() {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Optional.class, String.class));

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
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Optional.class, String.class));

			assertEquals(JavaType.valueOf(String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.object(), responseT.adapted(endpoint));
		}
	}

	@Test
	void compose(@Mock ResponseFn<String, String> fn, @Mock Promise<Response<String>> response) {
		Arguments arguments = Arguments.empty();

		when(fn.run(response, arguments)).thenReturn(Promise.done("expected"));

		Optional<String> optional = responseT.bind(endpoint, fn).join(response, arguments);

		assertAll(() -> assertTrue(optional.isPresent()), () -> assertEquals("expected", optional.get()));
	}
}
