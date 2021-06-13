package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
import com.github.ljtfreitas.julian.CompletionStageResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.JavaType.Parameterized;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;

@ExtendWith(MockitoExtension.class)
class CompletionStageResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private CompletionStageResponseT<String> responseT = new CompletionStageResponseT<>();

	@Nested
	class Predicates {

		@Nested
		class Supported {

			@ParameterizedTest
			@ArgumentsSource(AcceptableTypesProvider.class)
			void accepted(JavaType javaType) throws Exception {
				when(endpoint.returnType()).thenReturn(javaType);

				assertTrue(responseT.test(endpoint));
			}
		}

		@Test
		void unsupported() {
			when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

			assertFalse(responseT.test(endpoint));
		}
	}

	@Nested
	class Adapted {
	
		@ParameterizedTest
		@ArgumentsSource(AcceptableTypesProvider.class)
		void parameterized(JavaType javaType) throws Exception {
			when(endpoint.returnType()).thenReturn(javaType);

			JavaType expectedType = JavaType.valueOf(javaType.parameterized()
					.map(Parameterized::firstArg)
					.orElseThrow());

			assertEquals(expectedType, responseT.adapted(endpoint));
		}
	}
		
	@Test
	void compose(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request) throws Exception {
		Arguments arguments = Arguments.empty();

		when(request.comp(fn, arguments)).thenReturn(Promise.done("expected"));

		CompletionStage<String> completionStage = responseT.comp(endpoint, fn).join(request, arguments);

		assertEquals("expected", completionStage.toCompletableFuture().join());
	}

	static class AcceptableTypesProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) throws Exception {
			return Stream.of(org.junit.jupiter.params.provider.Arguments.of(JavaType.parameterized(CompletionStage.class, String.class)),
							 org.junit.jupiter.params.provider.Arguments.of(JavaType.parameterized(CompletableFuture.class, Throwable.class)));
		}
	}
}
