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
	
	private final QueueResponseT<String> responseT = new QueueResponseT<>();

	@Nested
	class Predicates {

		@Test
		void supported() {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Queue.class, String.class));

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
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Queue.class, String.class));

			assertEquals(JavaType.parameterized(Collection.class, String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.parameterized(Collection.class, Object.class), responseT.adapted(endpoint));
		}
	}

	@Nested
	class Responses {

		@Mock 
		private ResponseFn<Collection<String>, Collection<String>> fn;

		@Mock 
		private Promise<Response<Collection<String>>> response;
		
		@Test
		void compose() {
			when(fn.run(response, Arguments.empty())).thenReturn(Promise.done(List.of("expected")));

			Queue<String> queue = responseT.bind(endpoint, fn).join(response, Arguments.empty());

			assertAll(() -> assertThat(queue, contains("expected")), () -> assertEquals("expected", queue.poll()));
		}

		@Test
		void empty() {
			when(fn.run(response, Arguments.empty())).thenReturn(Promise.done(Collections.emptyList()));

			Queue<String> queue = responseT.bind(endpoint, fn).join(response, Arguments.empty());

			assertTrue(queue.isEmpty());
		}
	}
}
