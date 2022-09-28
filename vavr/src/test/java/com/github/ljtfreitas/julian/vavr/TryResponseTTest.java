package com.github.ljtfreitas.julian.vavr;

import io.vavr.control.Try;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TryResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final TryResponseT responseT = new TryResponseT();

    @Nested
    class Predicates {

        @Test
        void support() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Try.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));
            assertFalse(responseT.test(endpoint));
        }
    }

    @Nested
    class Adapt {

        @Test
        void adaptToArgument() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Try.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Try is not parameterized")
        void adaptToObjectWhenTryIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Try.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(Object.class)));
        }
    }

    @Test
    void bind() {
        String content = "hello";

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);
        Promise<Response<String, Throwable>> promise = Promise.done(Response.done(content));

        Try<Object> attempt = responseT.bind(endpoint, fn).join(promise, Arguments.empty());

        assertTrue(attempt.isSuccess());

        assertThat(attempt.get(), equalTo(content));
    }

    @Test
    void bindFailure() {
        RuntimeException failure = new RuntimeException("oops");

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);
        Promise<Response<String, Throwable>> promise = Promise.failed(failure);

        Arguments arguments = Arguments.empty();

        Try<Object> attempt = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(attempt.isFailure());

        Exception exception = (Exception) attempt.getCause();

        assertSame(failure, exception);
    }
}