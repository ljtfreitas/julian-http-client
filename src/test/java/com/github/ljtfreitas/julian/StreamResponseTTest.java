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

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.StreamResponseT;

@ExtendWith(MockitoExtension.class)
class StreamResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private StreamResponseT<String> responseT = new StreamResponseT<>();

	@Nested
	class Predicates {
		
		@Test
		void supported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Stream.class, String.class));

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
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Stream.class, String.class));

			assertEquals(JavaType.parameterized(Collection.class, String.class), responseT.adapted(endpoint));
		}

		@Test
		void simple() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.object());

			assertEquals(JavaType.parameterized(Collection.class, Object.class), responseT.adapted(endpoint));
		}
	}

	@Test
	void compose(@Mock ResponseFn<Collection<String>, Collection<String>> fn, @Mock RequestIO<Collection<String>> request) throws Exception {
		Arguments arguments = Arguments.empty();

		when(request.comp(fn, arguments)).thenReturn(Promise.done(List.of("expected")));

		Stream<String> stream = responseT.comp(endpoint, fn).join(request, arguments);

		assertThat(stream.collect(toList()), hasItem("expected"));
	}
}
