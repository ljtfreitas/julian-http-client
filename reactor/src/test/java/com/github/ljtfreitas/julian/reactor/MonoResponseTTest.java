package com.github.ljtfreitas.julian.reactor;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonoResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final MonoResponseT subject = new MonoResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Mono.class, String.class));

            assertTrue(subject.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(subject.test(endpoint));
        }
    }

    @Nested
    class Adapted {

        @Test
        void parameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Mono.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToObjectWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Mono.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind() {
        Promise<Response<String>> response = Promise.done(Response.done("hello"));

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Mono<Object> mono = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(mono)
                .expectNext("hello")
                .expectComplete()
                .verify();
    }

    @Test
    void bindAsMono() {
        Promise<Response<String>> response = new MonoPromise<>(Mono.just(Response.done("hello")));

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Mono<Object> mono = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(mono)
                .expectNext("hello")
                .expectComplete()
                .verify();
    }

    @Test
    void failure() {
        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<String>> response = Promise.failed(exception);

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Mono<Object> mono = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(mono)
                .expectErrorMatches(t -> t.equals(exception))
                .verify();
    }

    @Test
    void failureAsMono() {
        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<String>> response = new MonoPromise<>(Mono.error(exception));

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Mono<Object> mono = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(mono)
                .expectErrorMatches(t -> t.equals(exception))
                .verify();
    }
}