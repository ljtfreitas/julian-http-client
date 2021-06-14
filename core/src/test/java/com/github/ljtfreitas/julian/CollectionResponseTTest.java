package com.github.ljtfreitas.julian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.CollectionResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.JavaType.Parameterized;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;

@ExtendWith(MockitoExtension.class)
class CollectionResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private CollectionResponseT<String> responseT = new CollectionResponseT<>();

	@Nested
	class Predicates {

		@ParameterizedTest
		@ArgumentsSource(AcceptableCollectionsProvider.class)
		void supported() throws Exception {
			when(endpoint.returnType()).thenReturn(JavaType.parameterized(Collection.class, String.class));

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

		@ParameterizedTest
		@ArgumentsSource(AcceptableCollectionsProvider.class)
		void parameterized(JavaType javaType) throws Exception {
			when(endpoint.returnType()).thenReturn(javaType);

			JavaType expectedCollectionType = javaType.parameterized().map(Parameterized::firstArg)
					.map(a -> JavaType.parameterized(Collection.class, a))
					.orElseThrow();

			assertEquals(expectedCollectionType, responseT.adapted(endpoint));
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
		
		@ParameterizedTest
		@ArgumentsSource(AcceptableCollectionsProvider.class)
		void compose(JavaType javaType) throws Exception {
			when(endpoint.returnType()).thenReturn(javaType);

			when(request.comp(fn, Arguments.empty())).thenReturn(Promise.done(List.of("expected")));

			Collection<String> response = responseT.comp(endpoint, fn).join(request, Arguments.empty());

			assertThat(response, contains("expected"));
		}

		@ParameterizedTest
		@ArgumentsSource(AcceptableCollectionsProvider.class)
		void empty(JavaType javaType, Class<?> expectedCollectionType) throws Exception {
			when(endpoint.returnType()).thenReturn(javaType);

			when(request.comp(fn, Arguments.empty())).thenReturn(Promise.done(Collections.emptyList()));

			Collection<String> response = responseT.comp(endpoint, fn).join(request, Arguments.empty());

			assertAll(() -> assertTrue(response.isEmpty()),
					  () -> assertTrue(expectedCollectionType.isInstance(response))); 
		}
	}

	static class AcceptableCollectionsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) throws Exception {
			return Stream.of(arguments(JavaType.parameterized(Collection.class, String.class), Collection.class),
							 arguments(JavaType.parameterized(List.class, String.class), List.class),
							 arguments(JavaType.parameterized(Set.class, String.class), Set.class));
		}
	}
}
