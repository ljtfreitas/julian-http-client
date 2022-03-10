package com.github.ljtfreitas.julian.vavr;

import io.vavr.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FutureResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final FutureResponseT responseT = new FutureResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Future.class, String.class));
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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Future.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Future is not parameterized")
        void adaptToObjectWhenOptionIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Future.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(Object.class)));
        }
    }

    @Test
    void bind(@Mock Promise<Response<String>> promise, @Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        String content = "hello";

        when(fn.run(promise, arguments)).thenReturn(Promise.done(content));

        Future<Object> future = responseT.bind(endpoint, fn).join(promise, arguments);

        assertThat(future.get(), equalTo(content));
    }

    @Test
    void bindFailure(@Mock Promise<Response<String>> promise, @Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException failure = new RuntimeException("oops");

        when(fn.run(promise, arguments)).then(i -> Promise.failed(failure));

        Future<Object> future = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(future.isFailure());

        Object failureMessage = future.recover(Throwable::getMessage).get();

        assertThat(failureMessage, equalTo(failure.getMessage()));
    }
}