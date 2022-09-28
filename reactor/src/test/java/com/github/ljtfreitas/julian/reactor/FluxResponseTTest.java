package com.github.ljtfreitas.julian.reactor;

import com.github.ljtfreitas.julian.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluxResponseTTest {

    @Mock
    private Endpoint endpoint;
    
    private final FluxResponseT subject = new FluxResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flux.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flux.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, String.class), adapted);
        }

        @Test
        void adaptToCollectionWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Flux.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, Object.class), adapted);
        }
    }

    @Test
    void bind() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flux.class, String.class));

        Promise<Response<Collection<String>, Throwable>> response = Promise.done(Response.done(List.of("one", "two", "three")));

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Flux<Object> flux = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(flux)
                .expectNext("one", "two", "three")
                .expectComplete();
    }

    @Test
    void bindAsMono() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flux.class, String.class));

        Promise<Response<Collection<String>, Throwable>> response = new MonoPromise<>(
                Mono.just(Response.done(List.of("one", "two", "three"))));

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Flux<Object> flux = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(flux)
                .expectNext("one", "two", "three")
                .expectComplete()
                .verify();
    }

    @Test
    void failure() {
        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<Collection<String>, Throwable>> response = Promise.failed(exception);

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Flux<Object> flux = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(flux)
                .expectErrorMatches(t -> t.equals(exception))
                .verify();
    }

    @Test
    void failureAsMono() {
        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<Collection<String>, Throwable>> response = new MonoPromise<>(Mono.error(exception));

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Flux<Object> flux = subject.bind(endpoint, fn).join(response, Arguments.empty());

        StepVerifier.create(flux)
                .expectErrorMatches(t -> t.equals(exception))
                .verify();
    }
}