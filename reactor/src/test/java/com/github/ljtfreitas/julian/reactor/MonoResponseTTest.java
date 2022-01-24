package com.github.ljtfreitas.julian.reactor;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonoResponseTTest {

    private final MonoResponseT<String> subject = new MonoResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Mono.class, String.class));

            assertTrue(subject.test(endpoint));
        }

        @Test
        void unsupported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(subject.test(endpoint));
        }
    }

    @Nested
    class Adapted {

        @Test
        void parameterized(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Mono.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToObjectWhenTypeArgumentIsMissing(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Mono.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind(@Mock Endpoint endpoint, @Mock Promise<Response<String, Exception>, Exception> response, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(response, arguments)).thenReturn(Promise.done("hello"));

        Mono<String> mono = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(mono)
                .expectNext("hello")
                .expectComplete();
    }

    @Test
    void bindAsMono(@Mock Endpoint endpoint, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        Promise<Response<String, Exception>, Exception> response = new MonoPromise<>(Mono.just(Response.done("hello")));

        when(fn.run(response, arguments)).then(i -> response.then(r -> r.body().unsafe()));

        Mono<String> mono = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(mono)
                .expectNext("hello")
                .expectComplete();
    }

    @Test
    void failure(@Mock Endpoint endpoint, @Mock Promise<Response<String, Exception>, Exception> response, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException exception = new RuntimeException("oops");
        when(fn.run(response, arguments)).then(i -> Promise.failed(exception));

        Mono<String> mono = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(mono)
                .expectErrorMatches(t -> t.equals(exception));
    }

    @Test
    void failureAsMono(@Mock Endpoint endpoint, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<String, Exception>, Exception> response = new MonoPromise<>(Mono.error(exception));

        when(fn.run(response, arguments)).then(i -> response);

        Mono<String> mono = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(mono)
                .expectErrorMatches(t -> t.equals(exception));
    }
}