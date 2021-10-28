package com.github.ljtfreitas.julian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueueResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private QueueResponseT<String> responseT = new QueueResponseT<>();

	@Nested
	class Predicates {

		@Test
		void supported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Queue.class, String.class));

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
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Queue.class, String.class));

			assertEquals(JavaType.parameterized(Collection.class, String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.parameterized(Collection.class, Object.class), responseT.adapted(endpoint));
		}
	}

	@Nested
	class Responses {

		@Mock 
		private ResponseFn<Collection<String>, Collection<String>> fn;

		@Mock 
		private RequestIO<Collection<String>> request;
		
		@Test
		void compose() throws Exception {
			when(request.run(fn, Arguments.empty())).thenReturn(Promise.done(List.of("expected")));

			Queue<String> response = responseT.comp(endpoint, fn).join(request, Arguments.empty());

			assertAll(() -> assertThat(response, contains("expected")), () -> assertEquals("expected", response.poll()));
		}

		@Test
		void empty() throws Exception {
			when(request.run(fn, Arguments.empty())).thenReturn(Promise.done(Collections.emptyList()));

			Queue<String> response = responseT.comp(endpoint, fn).join(request, Arguments.empty());

			assertTrue(response.isEmpty());
		}
	}
}
