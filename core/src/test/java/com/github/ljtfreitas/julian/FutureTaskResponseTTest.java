package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.FutureTaskResponseT;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;

@ExtendWith(MockitoExtension.class)
class FutureTaskResponseTTest {

	@Mock
	private Endpoint endpoint;

	private FutureTaskResponseT<String> responseT = new FutureTaskResponseT<>();

	@Nested
	class Predicates {

		@Test
		void supported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(FutureTask.class, String.class));

			assertTrue(responseT.test(endpoint));
		}
		
		@Test
		void unsupported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

			assertFalse(responseT.test(endpoint));
		}
	}
	
	@Nested
	class Adapted {

		@Test
		void parameterized() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(FutureTask.class, String.class));

			assertEquals(JavaType.valueOf(String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.object(), responseT.adapted(endpoint));
		}
	}
	
	@Test
	void compose(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request) throws Exception {
		Arguments arguments = Arguments.empty();

		when(request.comp(fn, arguments)).thenReturn(Promise.done("expected"));

		FutureTask<String> task = responseT.comp(endpoint, fn).join(request, arguments);

		Executors.newSingleThreadExecutor().submit(task);

		assertEquals("expected", task.get());
	}
}
