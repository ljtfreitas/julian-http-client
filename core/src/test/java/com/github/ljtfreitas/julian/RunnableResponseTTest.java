package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunnableResponseTTest {

	@Mock
	private Endpoint endpoint;

	private final RunnableResponseT responseT = new RunnableResponseT();

	@Nested
	class Predicates {

		@Test
		void supported() {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Runnable.class, String.class));

			assertTrue(responseT.test(endpoint));
		}
		
		@Test
		void unsupported() {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

			assertFalse(responseT.test(endpoint));
		}
	}
	
	@Test
	void adapted() {
		assertEquals(JavaType.none(), responseT.adapted(endpoint));
	}

	@Test
	void compose(@Mock ResponseFn<Void, Void> fn, @Mock Promise<Response<Void, Throwable>> response) {
		Arguments arguments = Arguments.empty();

		Runnable runnable = responseT.bind(endpoint, fn).join(response, arguments);
		runnable.run();
		
		verify(fn).join(response, arguments);
	}
}
