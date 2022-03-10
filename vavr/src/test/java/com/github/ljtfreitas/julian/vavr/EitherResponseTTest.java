package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.vavr.control.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EitherResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final EitherResponseT<Exception> responseT = new EitherResponseT<>();

    @Nested
    class Predicates {

        @Test
        void support() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Exception.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @DisplayName("We should support any Exception type.")
        @Test
        void supportAnyExceptionTypes() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, RuntimeException.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));
            assertFalse(responseT.test(endpoint));
        }

        @DisplayName("We should not support Throwable class.")
        @Test
        void shouldNotSupportThrowable() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Throwable.class, String.class));
            assertFalse(responseT.test(endpoint));
        }
    }

    @Nested
    class Adapt {

        @Test
        void adaptToRight() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Exception.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Either is not parameterized")
        void adaptToObjectWhenEitherIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Either.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(Object.class)));
        }
    }

    @Test
    void bind(@Mock Promise<Response<String>> promise, @Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        String content = "hello";

        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Exception.class, String.class));
        when(fn.run(promise, arguments)).thenReturn(Promise.done(content));

        Either<Exception, Object> either = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(either.isRight());

        assertThat(either.get(), equalTo(content));
    }

    @Test
    void bindFailure(@Mock Promise<Response<String>> promise, @Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException failure = new RuntimeException("oops");

        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Exception.class, String.class));
        when(fn.run(promise, arguments)).then(i -> Promise.failed(failure));

        Either<Exception, Object> either = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(either.isLeft());

        Exception exception = either.swap().get();

        assertSame(failure, exception);
    }
}