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

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.RunnableResponseT;

@ExtendWith(MockitoExtension.class)
class RunnableResponseTTest {

	@Mock
	private Endpoint endpoint;

	private RunnableResponseT responseT = new RunnableResponseT();

	@Nested
	class Predicates {

		@Test
		void supported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Runnable.class, String.class));

			assertTrue(responseT.test(endpoint));
		}
		
		@Test
		void unsupported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

			assertFalse(responseT.test(endpoint));
		}
	}
	
	@Test
	void adapted() throws Exception {
		assertEquals(JavaType.none(), responseT.adapted(endpoint));
	}

	@Test
	void compose(@Mock RequestIO<Void> request) throws Exception {
		Arguments arguments = Arguments.empty();

		when(request.execute()).then(a -> Promise.done(Response.empty()));

		Runnable runnable = responseT.<Void> comp(endpoint, null).join(request, arguments);

		runnable.run();
		
		verify(request).execute();
	}
}
