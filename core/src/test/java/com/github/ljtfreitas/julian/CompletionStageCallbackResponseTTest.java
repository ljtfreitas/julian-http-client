package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.JavaType.Parameterized;

@ExtendWith(MockitoExtension.class)
class CompletionStageCallbackResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private CompletionStageCallbackResponseT<String> responseT = new CompletionStageCallbackResponseT<>();

	@Nested
	class Predicates {

		@Nested
		class Supported {

			@BeforeEach
			void setup() {
				when(endpoint.returnType()).thenReturn(JavaType.none());
			}

			@ParameterizedTest
			@ArgumentsSource(AcceptableCallbackParametersProvider.class)
			void accepted(Parameter parameter) throws Exception {
				when(endpoint.parameters()).thenReturn(new Parameters(List.of(parameter)));

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
		@ArgumentsSource(AcceptableCallbackParametersProvider.class)
		void parameterized(Parameter parameter) throws Exception {
			when(endpoint.parameters()).thenReturn(new Parameters(List.of(parameter)));

			JavaType expectedType = JavaType.valueOf(parameter.javaType().parameterized()
					.map(Parameterized::firstArg)
					.orElseThrow());

			assertEquals(expectedType, responseT.adapted(endpoint));
		}

		@Test
		void unsupported() throws Exception {
			when(endpoint.parameters()).thenReturn(Parameters.empty());

			assertEquals(JavaType.none(), responseT.adapted(endpoint));
		}
	}

	@Nested
	class Callbacks {

		@Test
		void consumesSuccess(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request, @Mock Consumer<String> success) throws Exception {
			when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "success", JavaType.parameterized(Consumer.class, String.class))));
			when(fn.returnType()).thenReturn(JavaType.valueOf(String.class));

			String expected = "expected";

			Consumer<String> check = r -> assertEquals(expected, r);

			Arguments arguments = Arguments.create(check.andThen(success));

			when(request.comp(fn, arguments)).thenReturn(Promise.done(expected));

			responseT.comp(endpoint, fn).join(request, arguments);

			verify(success).accept(expected);
		}

		@Test
		void consumesFailure(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request, @Mock Consumer<Throwable> failure) throws Exception {
			when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "failure", JavaType.parameterized(Consumer.class, Throwable.class))));
			when(fn.returnType()).thenReturn(JavaType.none());

			RuntimeException e = new RuntimeException("expected");

			Consumer<Throwable> check = t -> assertSame(e, t);

			Arguments arguments = Arguments.create(check.andThen(failure));

			when(request.comp(fn, arguments)).thenReturn(Promise.failed(e));

			responseT.comp(endpoint, fn).join(request, arguments);

			verify(failure).accept(e);
		}

		@Nested
		class WithBiConsumer {

			@Test
			void consumesSuccess(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request, @Mock BiConsumer<String, Throwable> subscriber) throws Exception {
				when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "success", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class))));
				when(fn.returnType()).thenReturn(JavaType.valueOf(String.class));

				String expected = "expected";

				BiConsumer<String, Throwable> check = (r, t) -> assertAll(() -> assertNull(t), () ->  assertEquals(expected, r));

				Arguments arguments = Arguments.create(check.andThen(subscriber));

				when(request.comp(fn, arguments)).thenReturn(Promise.done(expected));

				responseT.comp(endpoint, fn).join(request, arguments);

				verify(subscriber).accept(expected, null);
			}

			@Test
			void consumesFailure(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request, @Mock BiConsumer<String, Throwable> subscriber) throws Exception {
				when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "consumer", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class))));
				when(fn.returnType()).thenReturn(JavaType.valueOf(String.class));
	
				RuntimeException e = new RuntimeException("expected");

				BiConsumer<String, Throwable> check = (r, t) -> assertAll(() -> assertNull(r), () ->  assertSame(e, t));

				Arguments arguments = Arguments.create(check.andThen(subscriber));

				when(request.comp(fn, arguments)).thenReturn(Promise.failed(e));

				responseT.comp(endpoint, fn).join(request, arguments);

				verify(subscriber).accept(null, e);
			}
		}
	}


	static class AcceptableCallbackParametersProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) throws Exception {
			return Stream.of(org.junit.jupiter.params.provider.Arguments.of(Parameter.callback(0, "success", JavaType.parameterized(Consumer.class, String.class))),
							 org.junit.jupiter.params.provider.Arguments.of(Parameter.callback(0, "failure", JavaType.parameterized(Consumer.class, Throwable.class))),
							 org.junit.jupiter.params.provider.Arguments.of(Parameter.callback(0, "consumer", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class))));
		}
	}
}
