package com.github.ljtfreitas.julian;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private final StreamResponseT responseT = new StreamResponseT();

	@Nested
	class Predicates {
		
		@Test
		void supported() {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Stream.class, String.class));

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
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Stream.class, String.class));

			assertEquals(JavaType.parameterized(Collection.class, String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.parameterized(Collection.class, Object.class), responseT.adapted(endpoint));
		}
	}

	@Test
	void compose(@Mock ResponseFn<Collection<String>, Collection<Object>> fn, @Mock Promise<Response<Collection<String>, Throwable>> response) {
		Arguments arguments = Arguments.empty();

		when(fn.run(response, arguments)).thenReturn(Promise.done(List.of("expected")));

		Stream<Object> stream = responseT.bind(endpoint, fn).join(response, arguments);

		assertThat(stream.collect(toList()), hasItem("expected"));
	}
}
