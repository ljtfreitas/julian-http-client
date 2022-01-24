package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FutureTaskResponseTTest {

	@Mock
	private Endpoint endpoint;

	private final FutureTaskResponseT<String> responseT = new FutureTaskResponseT<>();

	@Nested
	class Predicates {

		@Test
		void supported() {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(FutureTask.class, String.class));

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
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(FutureTask.class, String.class));

			assertEquals(JavaType.parameterized(Callable.class, String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.object(), responseT.adapted(endpoint));
		}
	}
	
	@Test
	void compose(@Mock ResponseFn<String, Callable<String>> fn, @Mock Promise<Response<String, Exception>, Exception> response) throws Exception {
		Arguments arguments = Arguments.empty();

		when(fn.join(response, arguments)).thenReturn(() -> "expected");

		FutureTask<String> task = responseT.bind(endpoint, fn).join(response, arguments);

		Executors.newSingleThreadExecutor().submit(task);

		assertEquals("expected", task.get());
	}
}
