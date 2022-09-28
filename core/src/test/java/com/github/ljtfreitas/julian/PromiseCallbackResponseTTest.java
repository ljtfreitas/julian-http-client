package com.github.ljtfreitas.julian;

import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.JavaType.Parameterized;
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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromiseCallbackResponseTTest {

	@Mock
	private Endpoint endpoint;
	
	private final PromiseCallbackResponseT responseT = new PromiseCallbackResponseT();

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
			void accepted(Parameter parameter) {
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
		void parameterized(Parameter parameter) {
			when(endpoint.parameters()).thenReturn(new Parameters(List.of(parameter)));

			JavaType expectedType = JavaType.valueOf(parameter.javaType().parameterized()
					.map(Parameterized::firstArg)
					.orElseThrow());

			assertEquals(expectedType, responseT.adapted(endpoint));
		}

		@Test
		void unsupported() {
			when(endpoint.parameters()).thenReturn(Parameters.empty());

			assertEquals(JavaType.none(), responseT.adapted(endpoint));
		}
	}

	@Nested
	class Callbacks {

		@Test
		void consumesSuccess(@Mock ResponseFn<String, Object> fn, @Mock Promise<Response<String, Throwable>> response, @Mock Consumer<String> success) {
			when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "success", JavaType.parameterized(Consumer.class, String.class))));
			when(fn.returnType()).thenReturn(JavaType.valueOf(String.class));

			String expected = "expected";

			Consumer<String> check = r -> assertEquals(expected, r);

			Arguments arguments = Arguments.create(check.andThen(success));

			when(fn.run(response, arguments)).thenReturn(Promise.done(expected));

			responseT.bind(endpoint, fn).join(response, arguments);

			verify(success).accept(expected);
		}

		@Test
		void consumesFailure(@Mock ResponseFn<String, Object> fn, @Mock Promise<Response<String, Throwable>> response, @Mock Consumer<Throwable> failure) {
			when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "failure", JavaType.parameterized(Consumer.class, Throwable.class))));
			when(fn.returnType()).thenReturn(JavaType.none());

			RuntimeException e = new RuntimeException("expected");

			Consumer<Throwable> check = t -> assertSame(e, t);

			Arguments arguments = Arguments.create(check.andThen(failure));

			when(fn.run(response, arguments)).then(i ->  Promise.failed(e));

			responseT.bind(endpoint, fn).join(response, arguments);

			verify(failure).accept(e);
		}

		@Nested
		class WithBiConsumer {

			@Test
			void consumesSuccess(@Mock ResponseFn<String, Object> fn, @Mock Promise<Response<String, Throwable>> response, @Mock BiConsumer<String, Throwable> subscriber) {
				when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "success", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class))));
				when(fn.returnType()).thenReturn(JavaType.valueOf(String.class));

				String expected = "expected";

				BiConsumer<String, Throwable> check = (r, t) -> assertAll(() -> assertNull(t), () ->  assertEquals(expected, r));

				Arguments arguments = Arguments.create(check.andThen(subscriber));

				when(fn.run(response, arguments)).thenReturn(Promise.done(expected));

				responseT.bind(endpoint, fn).join(response, arguments);

				verify(subscriber).accept(expected, null);
			}

			@Test
			void consumesFailure(@Mock ResponseFn<String, Object> fn, @Mock Promise<Response<String, Throwable>> response, @Mock BiConsumer<String, Throwable> subscriber) {
				when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "consumer", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class))));
				when(fn.returnType()).thenReturn(JavaType.valueOf(String.class));
	
				RuntimeException e = new RuntimeException("expected");

				BiConsumer<String, Throwable> check = (r, t) -> assertAll(() -> assertNull(r), () ->  assertSame(e, t));

				Arguments arguments = Arguments.create(check.andThen(subscriber));

				when(fn.run(response, arguments)).then(i -> Promise.failed(e));

				responseT.bind(endpoint, fn).join(response, arguments);

				verify(subscriber).accept(null, e);
			}
		}
	}


	static class AcceptableCallbackParametersProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			return Stream.of(org.junit.jupiter.params.provider.Arguments.of(Parameter.callback(0, "success", JavaType.parameterized(Consumer.class, String.class))),
							 org.junit.jupiter.params.provider.Arguments.of(Parameter.callback(0, "failure", JavaType.parameterized(Consumer.class, Throwable.class))),
							 org.junit.jupiter.params.provider.Arguments.of(Parameter.callback(0, "consumer", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class))));
		}
	}
}
